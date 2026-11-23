/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ai;

import android.text.TextUtils;
import android.widget.Toast;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * يقترح ردًا على آخر رسالة واردة باستخدام أي خدمة متوافقة مع صيغة OpenAI chat/completions
 * (Groq، OpenRouter، Anthropic عبر /v1/messages، أو حتى خادم FreeLLMApi تشغّله بنفسك)،
 * ويضعه في خانة الكتابة دون إرساله تلقائيًا: المستخدم هو من يراجع ويعدّل ويضغط إرسال.
 * <p>
 * مفتاح الـAPI والعنوان (Base URL) يُخزَّنان محليًا فقط على هذا الجهاز ولا يُرسلان إلا
 * مباشرة إلى العنوان الذي اخترته أنت في الإعدادات.
 */
public class AyuAiReply {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // لمنع إرسال عدة طلبات متزامنة لنفس المحادثة إذا وصلت عدة رسائل سريعًا متتابعة.
    private static final Set<Long> pendingDialogs = Collections.synchronizedSet(new HashSet<>());

    private AyuAiReply() {
    }

    /**
     * يُستدعى من ChatActivity عند وصول رسائل جديدة للمحادثة المفتوحة حاليًا.
     */
    public static void maybeSuggestReply(ChatActivity activity, long dialogId, ArrayList<MessageObject> newMessages) {
        if (!AyuConfig.aiReplyEnabled) {
            return;
        }

        String apiKey = AyuConfig.getAiReplyApiKey();
        if (TextUtils.isEmpty(apiKey)) {
            return;
        }

        // dialogId سالب يعني مجموعة أو قناة؛ موجب يعني محادثة خاصة.
        if (dialogId < 0 && AyuConfig.aiReplyPrivateChatsOnly) {
            return;
        }

        MessageObject lastIncoming = null;
        for (int i = newMessages.size() - 1; i >= 0; i--) {
            MessageObject message = newMessages.get(i);
            if (message != null && !message.isOut() && !TextUtils.isEmpty(message.messageText)) {
                lastIncoming = message;
                break;
            }
        }
        if (lastIncoming == null) {
            return;
        }

        if (!pendingDialogs.add(dialogId)) {
            return; // طلب سابق لنفس المحادثة لم ينته بعد
        }

        String incomingText = lastIncoming.messageText.toString();
        String apiKeyFinal = apiKey;
        String baseUrl = AyuConfig.getAiReplyBaseUrl();
        String model = AyuConfig.getAiReplyModel();
        String systemPrompt = AyuConfig.getAiReplyPrompt();

        Utilities.globalQueue.postRunnable(() -> {
            String suggestion = null;
            String errorMessage = null;
            try {
                suggestion = callChatCompletions(baseUrl, apiKeyFinal, model, systemPrompt, incomingText);
            } catch (Exception e) {
                FileLog.e("AyuAiReply: request failed", e);
                errorMessage = e.getMessage();
            } finally {
                pendingDialogs.remove(dialogId);
            }

            if (!TextUtils.isEmpty(suggestion)) {
                String finalSuggestion = suggestion;
                AndroidUtilities.runOnUIThread(() -> applySuggestion(activity, dialogId, finalSuggestion));
            } else {
                String finalError = errorMessage;
                AndroidUtilities.runOnUIThread(() -> showFailureToast(finalError));
            }
        });
    }

    private static void showFailureToast(String errorMessage) {
        var context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        String text = "AI reply: " + (TextUtils.isEmpty(errorMessage) ? "failed (see logs)" : errorMessage);
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    private static void applySuggestion(ChatActivity activity, long dialogId, String suggestion) {
        if (activity == null || activity.isPaused() || activity.getDialogId() != dialogId) {
            return; // المستخدم غادر المحادثة أو فتح غيرها قبل أن يصل الرد
        }
        var enterView = activity.getChatActivityEnterView();
        if (enterView == null) {
            return;
        }
        var field = enterView.getEditField();
        if (field == null || field.length() > 0) {
            return; // لا تستبدل نصًا كتبه المستخدم بالفعل
        }
        field.setText(suggestion);
        field.setSelection(field.length());
    }

    /**
     * يتحدث بصيغة OpenAI chat/completions القياسية، التي تدعمها تقريبًا كل الخدمات المجانية
     * (Groq، OpenRouter بنماذجه المجانية :free، أو خادم FreeLLMApi تشغّله بنفسك محليًا).
     * {@code baseUrl} هو عنوان الخدمة بدون "/chat/completions" في آخره، مثل:
     * https://api.groq.com/openai/v1  أو  https://openrouter.ai/api/v1  أو  http://127.0.0.1:3001/v1
     */
    private static String callChatCompletions(String baseUrl, String apiKey, String model, String systemPrompt, String incomingText) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 300);

        JSONArray messages = new JSONArray();

        if (!TextUtils.isEmpty(systemPrompt)) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", incomingText);
        messages.put(userMessage);
        body.put("messages", messages);

        String url = (TextUtils.isEmpty(baseUrl) ? AyuConstants.DEFAULT_AI_REPLY_BASE_URL : baseUrl);
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url += "/chat/completions";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                String detail = extractErrorMessage(responseBody);
                FileLog.e("AyuAiReply: HTTP " + response.code() + " " + responseBody);
                throw new Exception("HTTP " + response.code() + (detail != null ? ": " + detail : ""));
            }

            JSONObject json = new JSONObject(responseBody);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            String text = message != null ? message.optString("content", null) : null;
            return text != null ? text.trim() : null;
        }
    }

    private static String extractErrorMessage(String responseBody) {
        if (TextUtils.isEmpty(responseBody)) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONObject error = json.optJSONObject("error");
            if (error != null) {
                return error.optString("message", null);
            }
        } catch (Exception ignored) {
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody;
    }
}
