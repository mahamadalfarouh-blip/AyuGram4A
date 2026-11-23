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
import com.radolyn.ayugram.AyuConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
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
 * يقترح ردًا على آخر رسالة واردة باستخدام Anthropic API، ويضعه في خانة الكتابة
 * دون إرساله تلقائيًا: المستخدم هو من يراجع ويعدّل ويضغط إرسال.
 * <p>
 * مفتاح الـAPI يُخزَّن محليًا فقط على هذا الجهاز ولا يُرسل إلا مباشرة إلى api.anthropic.com.
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
        String model = AyuConfig.getAiReplyModel();
        String systemPrompt = AyuConfig.getAiReplyPrompt();

        Utilities.globalQueue.postRunnable(() -> {
            String suggestion = null;
            try {
                suggestion = callAnthropic(apiKeyFinal, model, systemPrompt, incomingText);
            } catch (Exception e) {
                FileLog.e("AyuAiReply: request failed", e);
            } finally {
                pendingDialogs.remove(dialogId);
            }

            if (!TextUtils.isEmpty(suggestion)) {
                String finalSuggestion = suggestion;
                AndroidUtilities.runOnUIThread(() -> applySuggestion(activity, dialogId, finalSuggestion));
            }
        });
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

    private static String callAnthropic(String apiKey, String model, String systemPrompt, String incomingText) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 300);
        body.put("system", systemPrompt);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", incomingText);
        messages.put(userMessage);
        body.put("messages", messages);

        Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                FileLog.e("AyuAiReply: HTTP " + response.code());
                return null;
            }

            JSONObject json = new JSONObject(response.body().string());
            JSONArray content = json.optJSONArray("content");
            if (content == null || content.length() == 0) {
                return null;
            }
            String text = content.getJSONObject(0).optString("text", null);
            return text != null ? text.trim() : null;
        }
    }
}
