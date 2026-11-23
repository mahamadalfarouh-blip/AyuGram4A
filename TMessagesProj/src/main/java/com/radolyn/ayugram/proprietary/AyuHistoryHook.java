/*
 * Stub replacement for AyuGram's private "proprietary" submodule
 * (https://github.com/AyuGram/AyuGram4A_proprietary), which this personal
 * fork has no access to (it's team-private, and the fork's CI has none of
 * the upstream team's secrets/tokens to fetch it).
 *
 * This stub exists only so the public source compiles on a plain
 * GitHub-hosted runner. It intentionally no-ops: the proprietary
 * "message history" matching logic (used to reconcile deleted/edited
 * messages while scrolling chat history) is simply skipped here. That
 * does not affect the "AI reply suggestion" feature this fork adds; it
 * only means this particular AyuGram extra feature is inert in this build.
 */
package com.radolyn.ayugram.proprietary;

import android.util.Pair;
import android.util.SparseArray;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;

public class AyuHistoryHook {

    public static Pair<Integer, Integer> getMinAndMaxIds(ArrayList<MessageObject> messArr) {
        return new Pair<>(0, 0);
    }

    public static void doHook(int currentAccount, ArrayList<MessageObject> messArr, SparseArray<MessageObject>[] messagesDict,
                               int startId, int endId, long dialogId, int limit, int topicId, boolean isSecretChat) {
        // no-op stub
    }
}
