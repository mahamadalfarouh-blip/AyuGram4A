/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui.preferences;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.exteragram.messenger.preferences.BasePreferencesActivity;
import com.radolyn.ayugram.AyuConfig;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.ui.preferences.utils.AyuUi;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

public class AyuAiReplyPreferencesActivity extends BasePreferencesActivity {

    private int headerRow;
    private int enabledRow;
    private int privateOnlyRow;
    private int baseUrlRow;
    private int apiKeyRow;
    private int modelRow;
    private int promptRow;
    private int infoRow;

    @Override
    protected void updateRowsId() {
        super.updateRowsId();

        headerRow = newRow();
        enabledRow = newRow();
        privateOnlyRow = newRow();
        baseUrlRow = newRow();
        apiKeyRow = newRow();
        modelRow = newRow();
        promptRow = newRow();
        infoRow = newRow();
    }

    @Override
    public void onResume() {
        updateRowsId();
        super.onResume();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enabledRow) {
            AyuConfig.editor.putBoolean("aiReplyEnabled", AyuConfig.aiReplyEnabled ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.aiReplyEnabled);
        } else if (position == privateOnlyRow) {
            AyuConfig.editor.putBoolean("aiReplyPrivateChatsOnly", AyuConfig.aiReplyPrivateChatsOnly ^= true).apply();
            ((TextCheckCell) view).setChecked(AyuConfig.aiReplyPrivateChatsOnly);
        } else if (position == baseUrlRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.AiReplyBaseUrl),
                    AyuConfig::getAiReplyBaseUrl,
                    "aiReplyBaseUrl",
                    AyuConstants.DEFAULT_AI_REPLY_BASE_URL,
                    s -> {
                    },
                    String::trim
            );
        } else if (position == apiKeyRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.AiReplyApiKey),
                    AyuConfig::getAiReplyApiKey,
                    "aiReplyApiKey",
                    "",
                    s -> {
                    },
                    String::trim
            );
        } else if (position == modelRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.AiReplyModel),
                    AyuConfig::getAiReplyModel,
                    "aiReplyModel",
                    AyuConstants.DEFAULT_AI_REPLY_MODEL,
                    s -> {
                    },
                    String::trim
            );
        } else if (position == promptRow) {
            AyuUi.spawnEditBox(
                    getParentActivity(),
                    ((TextCell) view),
                    LocaleController.getString(R.string.AiReplyPrompt),
                    AyuConfig::getAiReplyPrompt,
                    "aiReplyPrompt",
                    AyuConstants.DEFAULT_AI_REPLY_PROMPT
            );
        }
    }

    @Override
    protected String getTitle() {
        return LocaleController.getString(R.string.AiReplyHeader);
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    private String maskedKey() {
        String key = AyuConfig.getAiReplyApiKey();
        if (key.isEmpty()) {
            return LocaleController.getString(R.string.AiReplyApiKeyNotSet);
        }
        if (key.length() <= 8) {
            return "••••••••";
        }
        return key.substring(0, 4) + "••••••••" + key.substring(key.length() - 4);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case 2:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == baseUrlRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AiReplyBaseUrl), AyuConfig.getAiReplyBaseUrl(), true);
                    } else if (position == apiKeyRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AiReplyApiKey), maskedKey(), true);
                    } else if (position == modelRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AiReplyModel), AyuConfig.getAiReplyModel(), true);
                    } else if (position == promptRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AiReplyPrompt), LocaleController.getString(R.string.AiReplyPromptValue), true);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == headerRow) {
                        headerCell.setText(LocaleController.getString(R.string.AiReplyHeader));
                    }
                    break;
                case 8:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == infoRow) {
                        infoCell.setText(LocaleController.getString(R.string.AiReplyInfo));
                    }
                    break;
                case 5:
                    TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                    textCheckCell.setEnabled(true, null);
                    if (position == enabledRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.AiReplyEnable), AyuConfig.aiReplyEnabled, true);
                    } else if (position == privateOnlyRow) {
                        textCheckCell.setTextAndCheck(LocaleController.getString(R.string.AiReplyPrivateOnly), AyuConfig.aiReplyPrivateChatsOnly, true);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == baseUrlRow || position == apiKeyRow || position == modelRow || position == promptRow) {
                return 2;
            } else if (position == headerRow) {
                return 3;
            } else if (position == infoRow) {
                return 8;
            }
            return 5;
        }
    }
}
