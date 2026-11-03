/*
 * Copyright 2026 Équipe Challengers (MESSAF).
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.libraries.designsystem.utils

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * MESSAF : bloque les captures et les enregistrements d'écran tant que cet écran est affiché ; Android ne
 * capture alors qu'une image noire. En le quittant, le réglage d'origine de la fenêtre est rétabli : la
 * protection posée par le code PIN, sur les anciennes versions d'Android, reste en place.
 */
@Composable
fun ProtegerEcran(actif: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, actif) {
        val window = activity.window
        val dejaProtege = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        val poser = actif && !dejaProtege
        if (poser) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (poser) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
