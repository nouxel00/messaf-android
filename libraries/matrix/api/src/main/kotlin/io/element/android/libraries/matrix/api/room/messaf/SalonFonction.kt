/*
 * Copyright 2026 Équipe Challengers (MESSAF).
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.element.android.libraries.matrix.api.room.messaf

import androidx.compose.runtime.Immutable
import io.element.android.libraries.matrix.api.core.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * MESSAF : un salon de fonction, tel que le décrit l'état `bf.messaf.salon` écrit par le bot de fonction.
 *
 * Un salon relie deux fonctions (par exemple DRH-SANTE et DH-SANTE). Le bot y tient la liste des titulaires
 * et des intérimaires de chaque fonction ; l'application s'en sert pour afficher « en qualité de ».
 */
@Immutable
data class SalonFonction(
    val fonctions: ImmutableList<FonctionDuSalon>,
    /** Niveau de classification du salon ; dès la Diffusion restreinte, l'écran est protégé. */
    val classification: Classification = Classification.NORMAL,
) {
    /** Vrai dès la Diffusion restreinte : captures d'écran bloquées, transfert et copie retirés. */
    val estClassifie: Boolean
        get() = classification != Classification.NORMAL

    /** La fonction que [userId] occupe dans ce salon, comme titulaire ou par intérim, ou null. */
    fun qualite(userId: UserId): Qualite? {
        fonctions.firstOrNull { userId in it.titulaires }?.let { return Qualite(it.libelle, interim = false) }
        fonctions.firstOrNull { f -> f.interimaires.any { it.userId == userId } }?.let { return Qualite(it.libelle, interim = true) }
        return null
    }

    /** Les intérims en cours dans ce salon. */
    val interims: List<Pair<FonctionDuSalon, InterimDuSalon>>
        get() = fonctions.flatMap { f -> f.interimaires.map { f to it } }

    companion object {
        /** Type de l'événement d'état, clé d'état vide. */
        const val TYPE = "bf.messaf.salon"

        /** Lit le contenu JSON de l'état ; null s'il ne décrit pas un salon de fonction. */
        fun parse(json: String): SalonFonction? = runCatching {
            val racine = Json.parseToJsonElement(json) as? JsonObject ?: return null
            if (racine.texte("type") != "fonction") return null
            val fonctions = (racine["fonctions"] as? JsonArray).orEmpty().mapNotNull { element ->
                val f = element as? JsonObject ?: return@mapNotNull null
                val poste = f.texte("poste") ?: return@mapNotNull null
                FonctionDuSalon(
                    poste = poste,
                    libelle = f.texte("libelle") ?: poste,
                    titulaires = (f["titulaires"] as? JsonArray).orEmpty()
                        .mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.versUserId() }
                        .toImmutableList(),
                    interimaires = (f["interimaires"] as? JsonArray).orEmpty()
                        .mapNotNull { i ->
                            val objet = i as? JsonObject ?: return@mapNotNull null
                            val userId = objet.texte("matrix_id")?.versUserId() ?: return@mapNotNull null
                            InterimDuSalon(userId = userId, jusquAu = objet.texte("jusqu_au"))
                        }
                        .toImmutableList(),
                )
            }
            SalonFonction(
                fonctions = fonctions.toImmutableList(),
                classification = Classification.depuis(racine.texte("classification")),
            ).takeIf { it.fonctions.isNotEmpty() }
        }.getOrNull()

        private fun JsonObject.texte(cle: String): String? = (this[cle] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private fun String.versUserId(): UserId? = runCatching { UserId(this) }.getOrNull()
    }
}

@Immutable
data class FonctionDuSalon(
    /** Code du poste, par exemple DRH-SANTE. */
    val poste: String,
    /** Intitulé du poste, par exemple « Directeur des ressources humaines ». */
    val libelle: String,
    val titulaires: ImmutableList<UserId>,
    val interimaires: ImmutableList<InterimDuSalon>,
)

@Immutable
data class InterimDuSalon(
    val userId: UserId,
    /** Dernier jour de l'intérim, au format AAAA-MM-JJ. */
    val jusquAu: String?,
)

/** Niveaux de classification des messages d'un salon, du moins au plus protégé. */
enum class Classification(val valeur: String) {
    NORMAL("normal"),
    DIFFUSION_RESTREINTE("diffusion_restreinte"),
    CONFIDENTIEL("confidentiel");

    companion object {
        /** Niveau lu dans l'état du salon ; un niveau inconnu est traité comme le plus protégé. */
        fun depuis(valeur: String?): Classification = when (valeur) {
            null, NORMAL.valeur -> NORMAL
            DIFFUSION_RESTREINTE.valeur -> DIFFUSION_RESTREINTE
            else -> CONFIDENTIEL
        }
    }
}

/** « en qualité de » : l'intitulé de la fonction, et s'il s'agit d'un intérim. */
data class Qualite(
    val libelle: String,
    val interim: Boolean,
)
