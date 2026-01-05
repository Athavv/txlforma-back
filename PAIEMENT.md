# Paiement Stripe

## Endpoints

- `POST /api/payments/create-intent?panierId={id}` - Créer un PaymentIntent
- `POST /api/payments/webhook` - Webhook Stripe (automatique)
- `POST /api/payments/sync` - Synchronisation manuelle (fallback uniquement)

## Tests en développement

### Ordre des opérations (IMPORTANT)

**1. Démarrer `stripe listen` EN PREMIER** (dans un terminal séparé) :

```bash
stripe listen --forward-to localhost:8080/api/payments/webhook
```

**⚠️ IMPORTANT :** Copiez le webhook secret affiché et mettez-le dans `application.properties` :

```properties
stripe.webhook-secret=whsec_xxx  # Remplacez par le secret affiché
```

Puis redémarrez votre application Spring Boot.

**2. Créer un PaymentIntent** :

```
POST /api/payments/create-intent?panierId={{panierId}}
```

**3. Compléter le paiement** :

```bash
stripe payment_intents confirm {{paymentIntentId}} \
  --payment-method=pm_card_visa \
  --return-url="https://example.com/return"
```

**✅ Si `stripe listen` est lancé, le webhook est envoyé automatiquement** et le panier passe en PAYE sans intervention. Vous n'avez PAS besoin d'appeler `/sync`.

**Note technique :** Le webhook lit maintenant le body brut pour éviter que Spring Boot ne transforme le JSON et casse la signature Stripe.

**4. Fallback (uniquement si le webhook n'a pas fonctionné)** :

```
POST /api/payments/sync
{
  "paymentIntentId": "pi_xxx"
}
```

**Note :** Si vous devez toujours utiliser `/sync`, c'est que `stripe listen` n'était pas lancé ou que le webhook secret ne correspond pas.

## 🔍 Débogage du webhook

Si le webhook ne fonctionne pas, vérifiez dans les logs de votre application :

1. **Vérifier que le webhook est reçu** :

   - Cherchez `"Webhook reçu"` dans les logs
   - Si absent, `stripe listen` n'envoie pas les événements

2. **Vérifier la signature** :

   - Si vous voyez `"Signature invalide"`, le webhook secret ne correspond pas
   - **Solution** : Relancez `stripe listen`, copiez le nouveau secret, mettez-le dans `application.properties`, redémarrez l'app

3. **Vérifier que le PaymentIntent existe** :

   - Si vous voyez `"Paiement introuvable"`, le PaymentIntent n'a pas été créé via votre API
   - **Solution** : Créez le PaymentIntent via `/api/payments/create-intent` avant de confirmer

4. **Commandes utiles** :

   ```bash
   # Voir les événements reçus par stripe listen
   stripe listen --print-json

   # Tester manuellement un événement
   stripe trigger payment_intent.succeeded
   ```

## Production

En production, **vous n'avez pas besoin de `stripe listen`**. Configurez le webhook dans le dashboard Stripe :

1. Allez sur https://dashboard.stripe.com/webhooks
2. Cliquez sur "Add endpoint"
3. Entrez votre URL de production : `https://votre-domaine.com/api/payments/webhook`
4. Sélectionnez les événements : `payment_intent.succeeded` et `payment_intent.payment_failed`
5. Copiez le "Signing secret" et mettez-le dans `application.properties`

Le frontend utilise Stripe.js pour collecter les informations de paiement. Une fois le paiement complété, Stripe envoie automatiquement le webhook à votre serveur.

## Configuration

```properties
stripe.secret-key=sk_test_xxx  # sk_live_xxx en production
stripe.webhook-secret=whsec_xxx  # whsec_xxx du dashboard Stripe
```
