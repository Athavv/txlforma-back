# TXLFORMA Backend

Backend Spring Boot pour l'application TXLFORMA - Gestion de formations.

## 📋 Prérequis

- Java 21
- Maven 3.9+
- MySQL 8.4+
- Compte Stripe (pour les paiements)

## 🚀 Déploiement sur Render

### 1. Créer une base de données MySQL

Dans Render Dashboard → New → Database → MySQL (Plan: Free)
Notez les credentials fournis.

### 2. Créer un Web Service

- Connectez votre repository GitHub `txlforma-backend`
- Environment: **Docker**
- Dockerfile Path: `Dockerfile`
- Plan: Free

### 3. Configurer les variables d'environnement

Dans Render → Environment Variables, ajoutez :

```env
SPRING_DATASOURCE_URL=jdbc:mysql://[HOST]:3306/txlforma_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=[USER]
SPRING_DATASOURCE_PASSWORD=[PASSWORD]
SPRING_JPA_HIBERNATE_DDL_AUTO=update
APP_JWT_SECRET=[Générez: openssl rand -base64 32]
APP_JWT_EXPIRATION_MS=86400000
STRIPE_SECRET_KEY=sk_live_[VOTRE_CLE_STRIPE]
STRIPE_WEBHOOK_SECRET=whsec_[VOTRE_SECRET_WEBHOOK]
APP_BASE_URL=https://[VOTRE-FRONTEND].vercel.app
APP_UPLOADS_DIRECTORY=uploads
APP_ATTESTATIONS_DIRECTORY=attestations
```

**⚠️ Important :**

- Générez un nouveau secret JWT pour la production
- Utilisez vos clés Stripe de **production** (`sk_live_xxx`)
- Configurez le webhook Stripe : `https://votre-backend.onrender.com/api/payments/webhook`

## 🔧 Développement local

### 1. Copier le fichier de configuration

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

### 2. Remplir `application.properties`

Éditez `application.properties` et remplacez les valeurs par vos credentials locaux :

- URL de la base de données MySQL locale
- Clés Stripe de test (`sk_test_xxx`)
- Secret JWT (générez-en un nouveau)

### 3. Démarrer l'application

```bash
./mvnw spring-boot:run
```

Ou avec Docker Compose :

```bash
docker-compose up
```

L'API sera accessible sur `http://localhost:8080/api`

## 📁 Structure

```
back/
├── src/main/java/          # Code source Java
├── src/main/resources/     # Configuration
│   ├── application.properties.example  # Template (à copier)
│   └── application.properties          # Votre config locale (NE PAS COMMITTER)
├── Dockerfile              # Pour Render
├── render.yaml            # Configuration Render
└── pom.xml                # Dépendances Maven
```

## 🔒 Sécurité

**⚠️ CRITIQUE :**

- `application.properties` contient vos secrets → **NE JAMAIS COMMITTER**
- `application.properties.example` est un template sans secrets → **OK à committer**
- En production, utilisez les variables d'environnement sur Render

## 📚 Documentation

- `DEPLOYMENT.md` - Guide complet de déploiement
- `QUICK_START.md` - Guide rapide (5 min)
- `TXLFORMA_API.postman_collection.json` - Collection Postman
# txlforma-back
