# 🗂️ 📚 Git — Les bases pour votre projet
## 🔑 Pourquoi utiliser Git ?

1. Sauvegarder votre code à chaque étape
2. Pouvoir revenir en arrière
3. Travailler à plusieurs sans conflits
4. Montrer votre progression (et prouver votre travail)

## 🚀 1. Installer Git

Windows / Mac / Linux:

👉 https://git-scm.com/

**Optionnel :** utilisez un outil graphique pour Git tel que [Fork](https://git-fork.com/) pour vous aider.

## 📁 2. Cloner le dépôt

```bash
git clone https://github.com/MonUniversite/MonProjet.git
cd MonProjet
```


## 🔧 3. Configurer Git

```bash
git config user.name "Prénom Nom"
git config user.email "votre.email@he-arc.ch"
```

## 📝 4. Créer une branche

```bash
git branch ma-fonction
git checkout ma-fonction
```

ou en une seule commande :

```bash
git checkout -b ma-fonction
```

## 💾 5. Ajouter, commit & push

```bash
# Vérifier l’état du dépôt
git status

# Ajouter des fichiers
git add User.java

# Faire un commit
git commit -m "Ajout de la classe User"

# Envoyer sur GitHub, sur votre branche
git push origin ma-fonction
```

## 🔀 6. Fusionner

* Sur GitHub: ouvrir une pull request pour fusionner votre branche sur la branche `master`.
* Décrire ce qui a été fait dans la PR.
* Gérer les éventuels conflits (et les résoudre)
* Relire et échanger sur les modifications de vos collègues, et corriger si nécessaire

## 🏷️ 7. Bonnes pratiques

✅ Des commits petits et fréquents

✅ Des messages clairs: `Ajoute fonction X`, `Corrige bug Y`

✅ Une branche = une fonctionnalité / correction

✅ Pas de commit géant "version finale"

## 🆘 8. Besoin d’aide ?

Quelques commandes utiles :

```bash
# Voir l'historique
git log

# Comparer les différences entre deux versions d'un même fichier
git diff monfichier.java

# Récupérer les modifications des autres
git fetch && git pull

# Sauvegarder temporairement vos modifications si vous devez changer de branche
git stash
```


## 📌 À retenir

💡 Votre dépôt Git, votre logbook et vos commits = votre preuve de travail.

Un code final sans preuve de démarche = 💀.
