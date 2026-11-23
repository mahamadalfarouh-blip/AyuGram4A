/*
 * Stub replacement for AyuGram's private "proprietary" submodule
 * (https://github.com/AyuGram/AyuGram4A_proprietary) — see AyuHistoryHook.java
 * in this same package for why this stub exists.
 *
 * These no-op stubs mean the "saved edits/deleted messages" database
 * entries this helper normally fills in will stay empty in this build.
 * The "AI reply suggestion" feature this fork adds does not depend on it.
 */
package com.radolyn.ayugram.proprietary;

import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.messages.AyuSavePreferences;
import org.telegram.tgnet.TLRPC;

public class AyuMessageUtils {

    public static void map(EditedMessage editedMessage, TLRPC.TL_message msg, int currentAccount) {
        // no-op stub
    }

    public static void mapMedia(EditedMessage editedMessage, TLRPC.TL_message msg) {
        // no-op stub
    }

    public static void map(AyuSavePreferences prefs, EditedMessage revision) {
        // no-op stub
    }

    public static void mapMedia(AyuSavePreferences prefs, EditedMessage revision, boolean mediaChanged) {
        // no-op stub
    }

    public static void map(AyuSavePreferences prefs, DeletedMessage deletedMessage) {
        // no-op stub
    }

    public static void mapMedia(AyuSavePreferences prefs, DeletedMessage deletedMessage, boolean saveMedia) {
        // no-op stub
    }
}
