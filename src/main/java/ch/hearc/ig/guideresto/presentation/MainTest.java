package ch.hearc.ig.guideresto.presentation;

import ch.hearc.ig.guideresto.business.*;
import ch.hearc.ig.guideresto.services.*;
import ch.hearc.ig.guideresto.persistence.ConnectionUtils;

import java.sql.Connection;
import java.sql.SQLException;
public class MainTest {
    public static void main(String[] args) {
            CompleteEvaluationMapper mapper = new CompleteEvaluationMapper();

            Restaurant resto = new Restaurant();
            resto.setId(1); // ⚠️ un restaurant existant

            CompleteEvaluation eval = new CompleteEvaluation(
                    new java.util.Date(),
                    resto,
                    "Super ambiance et service impeccable !",
                    "cedric.baudet"
            );

            CompleteEvaluation created = mapper.create(eval);
            if (created != null) {
                System.out.println("✅ Évaluation complète créée !");
                System.out.println("➡️ ID généré : " + created.getId());
                System.out.println("➡️ User : " + created.getUsername());
                System.out.println("➡️ Commentaire : " + created.getComment());

                boolean deleted = mapper.delete(created);
                if (deleted) {
                    System.out.println("🗑️ Évaluation supprimée avec succès !");
                } else {
                    System.out.println("⚠️ Échec de la suppression !");
                }
            } else {
                System.out.println("❌ Erreur lors de la création !");
            }
        }

    }

