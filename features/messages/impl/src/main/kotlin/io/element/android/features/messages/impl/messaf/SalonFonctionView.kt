/*
 * Copyright 2026 Équipe Challengers (MESSAF).
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.features.messages.impl.messaf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.actionlist.model.TimelineItemAction
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.messaf.Classification
import io.element.android.libraries.matrix.api.room.messaf.SalonFonction
import kotlinx.collections.immutable.ImmutableMap

/**
 * MESSAF : ce que l'écran d'un salon affiche d'un salon de fonction.
 *
 * @property salon l'état `bf.messaf.salon` du salon, tenu par le bot de fonction.
 * @property noms le nom affiché de chaque intérimaire.
 */
@Immutable
data class SalonFonctionAffichage(
    val salon: SalonFonction,
    val noms: ImmutableMap<UserId, String>,
)

/** Le salon de fonction affiché, lu par le nom des auteurs du fil (« en qualité de ») et la liste des actions. */
val LocalSalonFonction = compositionLocalOf<SalonFonction?> { null }

/** Actions retirées dans un salon classé : elles feraient sortir le contenu du salon. */
val ACTIONS_INTERDITES_SALON_CLASSE = setOf(
    TimelineItemAction.Forward,
    TimelineItemAction.CopyText,
    TimelineItemAction.CopyCaption,
    TimelineItemAction.CopyLink,
    TimelineItemAction.ViewSource,
)

/** « · Directeur des ressources humaines » ou « · Directeur des ressources humaines (intérim) », ou null. */
@Composable
fun qualiteExpediteur(senderId: UserId): String? {
    val qualite = LocalSalonFonction.current?.qualite(senderId) ?: return null
    val libelle = if (qualite.interim) stringResource(R.string.messaf_qualite_interim, qualite.libelle) else qualite.libelle
    return "· $libelle"
}

/** Bandeau en tête du fil : les deux fonctions du salon et les intérims en cours. */
@Composable
fun BandeauSalonFonction(
    affichage: SalonFonctionAffichage,
    modifier: Modifier = Modifier,
) {
    val fonctions = affichage.salon.fonctions.joinToString(" ↔ ") { it.libelle }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = ElementTheme.colors.bgCanvasDefault),
    ) {
        Row(
            modifier = Modifier
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = CompoundIcons.UserProfile(),
                contentDescription = null,
                tint = if (affichage.salon.estClassifie) ElementTheme.colors.iconCriticalPrimary else ElementTheme.colors.iconSecondary,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val niveau = when (affichage.salon.classification) {
                    Classification.NORMAL -> null
                    Classification.DIFFUSION_RESTREINTE -> R.string.messaf_classification_diffusion_restreinte
                    Classification.CONFIDENTIEL -> R.string.messaf_classification_confidentiel
                }
                if (niveau != null) {
                    Text(
                        text = stringResource(niveau),
                        style = ElementTheme.typography.fontBodySmMedium,
                        color = ElementTheme.colors.textCriticalPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.messaf_salon_fonction, fonctions),
                    style = ElementTheme.typography.fontBodySmMedium,
                    color = ElementTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                affichage.salon.interims.forEach { (fonction, interim) ->
                    val nom = affichage.noms[interim.userId] ?: interim.userId.extractedDisplayName
                    val jusquAu = interim.jusquAu?.let(::dateCourte)
                    Text(
                        text = if (jusquAu != null) {
                            stringResource(R.string.messaf_interim_jusqu_au, nom, fonction.libelle, jusquAu)
                        } else {
                            stringResource(R.string.messaf_interim, nom, fonction.libelle)
                        },
                        style = ElementTheme.typography.fontBodySmRegular,
                        color = ElementTheme.colors.textPrimary,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}

/** AAAA-MM-JJ en JJ/MM/AAAA, sans dépendre du fuseau horaire. */
private fun dateCourte(date: String): String {
    val morceaux = date.take(10).split("-")
    return if (morceaux.size == 3) "${morceaux[2]}/${morceaux[1]}/${morceaux[0]}" else date
}
