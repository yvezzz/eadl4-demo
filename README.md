# EADL4 — Pipeline CI/CD & Sécurité DevSecOps

**Auteur :** Yvan Tambat — **GitHub :** yvezzz  
**Contexte :** 150 développeurs, 2 sites (Paris + Lyon), infra hybride on-premise + cloud AWS.

---

## Livrable 1 — Architecture réseau

### Découpage en zones (VLANs)

Le réseau est découpé en 7 zones, chacune isolée des autres :

| Zone | Pour qui / quoi ? |
|------|-------------------|
| **LAN Dev Paris** | Postes des développeurs (site Paris) |
| **LAN Dev Lyon** | Postes des développeurs (site Lyon, via VPN) |
| **CI/CD** | Serveurs qui exécutent le pipeline (GitHub Runner, Registry Docker) |
| **Staging** | Environnement de test avant la production |
| **DMZ** | Zone accessible depuis Internet (reverse proxy Nginx, load balancer HAProxy) |
| **Production** | Serveurs qui hébergent l'application finale |
| **Administration** | Accès SSH réservé aux admins (Yvan, Dimitry) |

### Règles de circulation

- **Internet** peut seulement accéder à la DMZ (port 443 HTTPS)
- **Les développeurs** peuvent pusher leur code vers la zone CI/CD
- **Le pipeline CI/CD** peut déployer automatiquement sur le Staging
- **Les admins** peuvent se connecter en SSH à toutes les zones
- **Tout le reste est bloqué** — si ce n'est pas autorisé, c'est interdit

### Pourquoi ce découpage ?

Si un attaquant pirate une zone, il ne peut pas accéder aux autres. Par exemple, même si la zone CI/CD est compromise, la production reste protégée.

### Flux réseau du pipeline (étape par étape)

| Étape | Trafic réseau |
|-------|---------------|
| 1. Le développeur pousse son code | Dev → GitHub (HTTPS) |
| 2. GitHub Actions lance le runner | Runner GitHub → télécharge le code |
| 3. Build et tests | Interne à la zone CI/CD |
| 4. L'image Docker est construite et scannée | Runner → Registry Docker (interne) |
| 5. Déploiement sur staging | Runner → Serveur Staging (SSH) |
| 6. Déploiement en production (manuel) | Admin → Serveurs Production (SSH) |

**Validation :** Vérifier qu'un push sur `develop` déclenche bien le pipeline et que l'app apparaît sur `staging.app.internal`.

---

## Livrable 2 — Configuration réseau

Chaque équipement réseau est configuré avec des fichiers prêts à l'emploi (situés dans le dossier `eadl4-pipeline/`).

### Ce qui est configuré

| Équipement | Rôle |
|------------|------|
| **Switch** | Relie physiquement les machines et les répartit dans les bons VLANs |
| **Firewall (iptables)** | Applique les règles de filtrage : tout bloquer, ouvrir uniquement ce qui est nécessaire |
| **DNS (CoreDNS)** | Permet aux machines de se trouver par nom (ex: `app.internal` → adresse IP de la prod) |
| **Reverse proxy (Nginx)** | Point d'entrée unique depuis Internet, redirige vers staging ou prod selon le nom du site |
| **Load balancer (HAProxy)** | Répartit la charge entre les 2 serveurs de production |
| **VPN (WireGuard)** | Tunnel chiffré entre Paris et Lyon pour que les deux sites soient sur le même réseau |
| **Monitoring (Prometheus)** | Surveille l'état de chaque serveur et alerte si un serveur tombe |

### Vérification

Chaque configuration est testable avec une commande simple (ex: `show vlan brief` pour vérifier les VLANs, `curl https://staging.app.internal` pour tester le proxy).

---

## Livrable 3 — Pipeline CI/CD

### Outil choisi : GitHub Actions

GitHub Actions est gratuit, intégré à GitHub, et dispose de nombreuses actions prêtes à l'emploi (Trivy, SonarCloud, déploiement SSH).

### Fonctionnement du pipeline (6 étapes)

1. **Build** — Le code est compilé (Java 17, Maven)
2. **Tests unitaires** — Les tests sont lancés. Si la couverture de code est inférieure à 80%, le pipeline s'arrête
3. **Analyse qualité (SonarCloud)** — SonarCloud analyse le code pour détecter bugs, failles de sécurité et mauvaises pratiques. Si un bug bloquant est trouvé, le pipeline s'arrête
4. **Build image Docker + scan** — L'application est empaquetée dans une image Docker. Trivy scanne l'image pour détecter des vulnérabilités. Si une vulnérabilité critique est trouvée, le pipeline s'arrête et l'image n'est pas publiée
5. **Déploiement staging (automatique)** — Quand on merge sur `develop`, l'image est déployée automatiquement sur l'environnement de staging
6. **Déploiement production (manuel)** — Quand on crée un tag `v1.0`, on peut déclencher manuellement le déploiement en production

### Workflow Git

- **feature/*** → les développeurs créent une branche pour chaque fonctionnalité
- **develop** → on merge les fonctionnalités via des Merge Requests
- **main** → on tagge une version quand tout est validé

Les branches `main` et `develop` sont protégées : personne ne peut push directement, il faut une Merge Request approuvée par 2 personnes.

### Stratégie de déploiement : Rolling update

On met à jour les serveurs un par un. Pendant la mise à jour du serveur 1, le serveur 2 continue de servir les utilisateurs. Le load balancer bascule automatiquement.

---

## Livrable 4 — Sécurité DevSecOps

### 7 pratiques de sécurité appliquées

| Pratique | Qu'est-ce que ça fait ? |
|----------|--------------------------|
| **1. Gestion des secrets** | Les mots de passe, clés SSH et tokens ne sont jamais écrits dans le code. Ils sont stockés dans les secrets GitHub (Settings → Secrets) |
| **2. Scan SAST (SonarCloud)** | À chaque push, SonarCloud analyse le code source pour trouver des failles de sécurité avant même que le code ne soit déployé |
| **3. Scan des dépendances** | SonarCloud vérifie aussi les bibliothèques utilisées (fichier pom.xml) et signale si l'une d'elles a une vulnérabilité connue |
| **4. Scan d'image Docker (Trivy)** | Avant de publier l'image Docker, Trivy la scanne. Si une vulnérabilité critique est détectée, l'image n'est pas publiée et le pipeline est stoppé |
| **5. Contrôle d'accès (RBAC)** | Les administrateurs ont tous les droits, les développeurs peuvent seulement écrire sur leur branche et proposer des Merge Requests. La production est protégée |
| **6. Audit et traçabilité** | GitHub garde une trace de tout : qui a push, qui a approuvé une MR, qui a déployé, quand. Rien n'est fait anonymement |
| **7. Durcissement réseau** | Le firewall bloque tout par défaut. SSH est protégé par fail2ban (3 tentatives max avant bannissement). Seuls les ports nécessaires sont ouverts |

### Principe "Fail fast"

Le pipeline s'arrête immédiatement si l'une de ces conditions n'est pas respectée :
- Couverture de tests < 80%
- Bug bloquant détecté par SonarCloud
- Vulnérabilité critique dans l'image Docker
- Échec du déploiement sur staging

Cela permet de détecter les problèmes tôt et d'éviter de déployer du code dangereux en production.

**Validation :** Vérifier sur GitHub (Actions → runs) que chaque étape passe au vert. Pour la sécu, aller sur SonarCloud voir le quality gate et sur le repo vérifier que les branches sont protégées.

---

## Structure du projet

```
eadl4-pipeline/
├── .github/workflows/ci-cd.yml     # Pipeline GitHub Actions
├── pom.xml                          # Projet Spring Boot + JaCoCo + SonarCloud
├── Dockerfile                       # Construction de l'image Docker
├── src/main/java/com/example/demo/  # Code de l'application (API CRUD Users)
└── src/test/java/com/example/demo/  # Tests unitaires (100% de couverture)
```

---