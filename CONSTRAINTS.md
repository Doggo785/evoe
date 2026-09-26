# CONSTRAINTS.md - barre qualite Evoe

Last reviewed: 2026-09-26, avec l'auteur (projet solo).

Lire avant d'ecrire du code. Ne jamais affaiblir ce fichier pour faire passer un changement.

## Plancher (toujours applique, sans installation)

- Pas de nouveau commentaire de suppression : `@Suppress(`, `//noinspection`, `detekt:off`, `gitleaks:allow`, `nosemgrep`.
- Pas de stub non implemente : `TODO(`, `NotImplementedError`, `catch` vide, `TODO` la ou l'implementation devrait etre.
- Pas de test saute ou supprime sans motif dans le message de commit : `@Ignore`, `Assume.` supprimes ou ajoutes pour contourner.
- Pas d'assertion retiree d'un test qui reste (fichiers `*Test*` : `assert*`, `fail(`).
- Pas de secret en source : token Deezer, mot de passe keystore, `google-services.json` (gitignore, le build marche sans).
- Ce fichier ne se resserre qu'en silence ; tout assouplissement est bruyant (voir Garde du plancher).

## Applique, avec nombres

| Dimension | Regle | Verifie par | Quand | Pourquoi ce nombre |
|---|---|---|---|---|
| Compilation | Zero erreur Kotlin | `./gradlew :app:assembleDebug` | fin de tache, CI | Le compilateur est l'avis le plus fiable et le moins cher |
| Lint | Zero erreur detekt (config defaut) | `./gradlew detekt` | fin de tache (incremental < 90s), CI | Detekt 1.23.8 PSI-only est le seul analyseur qui couvre les 3 modules ; Lint Android a `abortOnError = false` et ne prouve rien ici |
| Secrets | Aucun secret nouveau en source | tache : `gitleaks protect --redact --no-banner --staged` ; CI : `gitleaks detect --redact --no-banner --baseline-path .gitleaksbaseline` | chaque tache (< 5s, staged uniquement), CI (historique complet) | Externe (base de signatures). `--redact` obligatoire : sans lui le secret fuit dans le transcript de l'agent. Le baseline fige les 4 constats connus (E1, E2) : seul un leak NOUVEAU echoue |
| Couverture lignes changees | >= baseline mesuree, cible 80% | `./gradlew test koverXmlReport` croise avec `git diff` | fin de tache, CI | 80% force un test sans bloquer une ligne de config ; le cliquet (baseline d'abord) evite un build rouge permanent sur un codebase non mesure |
| Vuln dependances | Rien >= high dans le runtime shipe | `./gradlew cyclonedxBom` puis `osv-scanner scan --sbom <module>/build/reports/cyclonedx/bom.json` (un `--sbom` par module : `app`, `common`, `deezer-extension/ext`) | CI | Externe (base de vulns). Sous high, c'est du bruit. `scan source -r .` ne marche pas ici (pas de lockfile) : le SBOM est la seule entree valable. Premier run 2026-09-26 : 105 vulns mais SBOM par defaut trop large (freemarker, jackson, netty x2 versions, bcprov, absents de `releaseRuntimeClasspath`) : le SBOM doit etre restreint au runtime release avant que ce gate bloque quoi que ce soit |
| ABI :common | `checkKotlinAbi` vert, dump commite | `./gradlew :common:checkKotlinAbi` (+ `:common:updateKotlinAbi` apres un changement voulu, diff de `common/api/jvm/common.api` commite avec) | a chaque touche a `:common`, CI | Les extensions lient contre cette ABI ; un changement silencieux casse tout le parc hors repo |
| ABI minifiee | `verifyExtensionAbi` vert | `./gradlew :app:assembleRelease` (le garde est en finalizer de `minify*WithR8`) | CI / avant release | R8 a deja casse toutes les extensions tierces d'un coup ; le garde echoue au build au lieu du terrain |
| Sortie Kotlin propre | `verifyCleanKotlinOutput` vert | dependance de `assembleRelease` | CI | Code inline stale apres changement d'une fonction inline publique |
| Hygiene deps | Nouvelles deps via `gradle/libs.versions.toml` uniquement, Coil >= 3.6.0 | revue + `./gradlew build` | revue | Sous Coil 3.6.0, R8 fusionne `GenericViewTarget` et l'artwork sort vierge en builds minifies uniquement |

## Mesure, pas encore applique

| Metrique | Aujourd'hui | Direction |
|---|---|---|
| Couverture projet (kover, lignes, 2026-09-26) | `:app` 0,2% (56/24087), `:common` 0,0% (0/787), `:deezer-extension` 5,0% (118/2377) | ne doit pas baisser (tolerance 0,5% de derive) |
| Taille APK release | non mesurée, bloquée par `:common:checkKotlinAbi` (voir rapport 2026-09-26 : dump ABI stale depuis `55a49365`) | ne doit pas croitre |

## Abandonne explicitement (pas de check invente)

- Semgrep : non installe ; detekt + revue suffisent pour l'instant.
- Perf type Lighthouse et accessibilite type axe : besoin d'une URL web servie ; ici app native, la mesure demande un appareil branche. Pas de dimension sans outil derriere.

## Cycle de vie (ou tourne quoi)

| Phase | Commande | Budget |
|---|---|---|
| EDIT | `gitleaks protect --redact --no-banner --staged` | < 5s |
| FIN DE TACHE (agent, bloquant) | `./gradlew detekt` + `./gradlew test` + plancher ci-dessous | <= 90s en incremental |
| CI / RELEASE | tout le tableau Applique + `assembleRelease` (gardes R8/ABI) | illimite |

Regle de cout : tout ce qui depasse quelques secondes sort de la boucle d'edition. On scope au diff : couverture des lignes touchees, pas du repo.

## Garde du plancher (adaptation Kotlin du reference `floor-guard.md`)

Sur le diff merge-base...HEAD (lignes ajoutees + retirees + fichiers untracked), signaler :

1. Seuil baisse : un nombre de ce fichier revu a la baisse, ou une ligne du Plancher supprimee.
2. Test facilite : `@Ignore` / `Assume` ajoute, fichier `*Test*` supprime, `assert*`/`fail(` retire d'un test qui reste.
3. Checker muselé : `@Suppress(`, `noinspection`, `detekt:off`, `gitleaks:allow` nouveau.
4. Travail inacheve : `TODO(`, `NotImplementedError`, `catch` vide.
5. Exception apparue : nouvelle ligne au tableau Exceptions sans discussion.

Codes : `0` propre, `1` violation (bloque), `2` garde inexecutable (pas de merge-base). Ne jamais lire `2` comme `0`. Durcir est silencieux, assouplir est bruyant.

## Exceptions

| ID | Regle | Chemin | Motif | Owner | Expire |
|---|---|---|---|---|---|
| E1 | `generic-api-key` | `deezer-extension/.../extension/Utils.kt:15` (`SECRET`, fragment Blowfish Deezer) | Constante publique documentee du chiffrement legacy Deezer, pas un credential personnel, non rotatable, requise au dechiffrement. Figee dans `.gitleaksbaseline`. | auteur | 2026-12-26 |
| E2 | `gcp-api-key` x3 | historique git `app/google-services.json` (commits `2282f3f`, `e93c64e`) | Fichier absent de l'arbre actuel (gitignore, build sans). Reste : verifier que ces cles Firebase sont revoquees/inactives, sinon les roter. Fige dans `.gitleaksbaseline`. | auteur | 2026-12-26 |

Duree de vie max d'une exception : 90 jours. Sans owner ni echeance, c'est un refus.
