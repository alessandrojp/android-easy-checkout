# Android In-App Billing Library
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Fast and easy to use Android In-app Billing Library.

This library supports both non-consumable/consumable items and upgrading/downgrading of subscriptions.

To do upgrade/downgrade of a subscription you must use the api version 5.

# Let's get started
### Installation
* Supports Android 2.2 SDK or higher.

* For Eclipse users, download the latest jar version from [here](https://github.com/alessandrojp/android-inapp-billing/releases) and add it as a dependency

* For Gradle users, add this into your build.gradle file:

  ```groovy
  repositories {
      mavenCentral()
  }
  dependencies {
     compile 'jp.alessandro.android.iab:library:1.0.+'
  }
  ```

* This library requires `com.android.vending.BILLING` permission. Usually, permissions are merged automatically during the manifest merging step, but you can add it into the *AndroidManifest.xml* of your application.

  ```xml
    <uses-permission android:name="com.android.vending.BILLING" />
  ```

* If you use Proguard to obfuscate your code, you must add the following lines to your Proguard configuration file:
  ```xml
  -keep class com.android.vending.billing.**
  -keep class jp.alessandro.android.iab.** {*;}
  ```

### Creating a Billing Context
* You need a **BillingContext** object to execute actions in this library.
You can create one as follows:

  ```java
  // Application context
  Context context = getApplicationContext();

  // Public key generated on the Google Play Console
  String base64EncodedPublicKey = "YOUR_PUBLIC_KEY";

  // You can choose which thread do you want to execute all actions in the library
  // I don't recommend to use the MainThread, sometimes you can get ANR calling getPurchases
  // More info: https://developer.android.com/google/play/billing/billing_integrate.html
  HandlerThread thread = new HandlerThread("BillingTest");
  thread.start();
  mActionHandler = new Handler(thread.getLooper());

  // Handler for the events
  // If you want to receive the events at the same thread of the actions,
  // you can set mActionHandler above
  Handler mEventHandler = new Handler();

  BillingContext context = new BillingContext(
                context,
                base64EncodedPublicKey,
                BillingApi.VERSION_5, // It also supports version 3
                mActionHandler,
                mEventHandler,
                new SystemLogger() // If don't want to check the logs, you can just give null
  ```

* See the sample of how to use it:

  ```java
  public class SampleActivity extends Activity {
      // Handler for the events
      private final Handler mEventHandler = new Handler();

      private ItemProcessor mItemProcessor;
      private SubscriptionProcessor mSubscriptionProcessor;

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_main);

          initBillingProcessor();
      }

      private void initBillingProcessor() {
          // You can choose which thread do you want to execute all actions in the library
          // I don't recommend to use the MainThread, sometimes you can get ANR calling getPurchases
          // More info: https://developer.android.com/google/play/billing/billing_integrate.html
          HandlerThread thread = new HandlerThread("BillingTest");
          thread.start();
          mActionHandler = new Handler(thread.getLooper());

          // Your public key
          String base64EncodedPublicKey = "YOUR_PUBLIC_KEY";

          BillingContext context = new BillingContext(
                  getApplicationContext(), // App context
                  base64EncodedPublicKey, // Public key generated on the Google Play Console
                  BillingApi.VERSION_5, // It also supports version 3
                  mActionHandler, // Handler for the library actions
                  mEventHandler, // Handler for the responses
                  new SystemLogger() // If don't want to check the logs, you can just send null
          );
          mItemProcessor = new ItemProcessor(context);
          mSubscriptionProcessor = new SubscriptionProcessor(context);
      }
    }
  }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mActionHandler != null) {
            mActionHandler.removeCallbacksAndMessages(null);
            // IMPORTANT: Remember to call quit() method.
            mActionHandler.getLooper().quit();
        }
        mEventHandler.removeCallbacksAndMessages(null);
    }
  ```

* Don't forget to override Activity's onActivityResult method like this:

  ```java
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      // For subscriptions
      if (mSubscriptionProcessor.handleActivityResult(requestCode, resultCode, data)) {
          // The response will be sent through PurchaseHandler

          // For consumables/non-consumables
      } else if (mItemProcessor.handleActivityResult(requestCode, resultCode, data)) {
          // The response will be sent through PurchaseHandler
      }
  }
  ```

# Purchase Item
* Call `ItemProcessor#purchase` method to purchase a consumable or non-consumable item and call `SubscriptionProcessor#purchase` to pucharse a subscription like this:

  ```java
  mItemProcessor.purchase(this, "YOUR_ITEM_SKU", "DEVELOPER_PAYLOAD", new PurchaseHandler() {
      @Override
      public void onSuccess(Purchase purchase) {
          // Do your stuff with the purchased item
      }

      @Override
      public void onError(BillingException e) {
          // Handle the error
      }
  });
  mSubscriptionProcessor.purchase(this, "YOUR_ITEM_SKU", "DEVELOPER_PAYLOAD", new PurchaseHandler() {
      @Override
      public void onSuccess(Purchase purchase) {
          // Do your stuff with the purchased item
      }

      @Override
      public void onError(BillingException e) {
          // Handle the error
      }
  });
  ```
As a result you will get a [Purchase](#purchase-object) object.

# Consume Purchased Item
* You can consume a purchased item anytime and allow the user to buy the same item again. To do this call `ItemProcessor.consumePurchase` like this:

  ```java
  mItemProcessor.consume("YOUR_ITEM_SKU", new ConsumeItemHandler() {
      @Override
      public void onSuccess(String itemId) {
        // Do your stuff
      }

      @Override
      public void onError(BillingException e) {
        // Handle the error
      }
  });
  ```

# Upgrade/Downgrade Subscription
* You can upgrade/downgrade an existing subscription by calling `ItemProcessor.consumePurchase` like this:
**You must use the api version 5 to do this**

  ```java
  ArrayList<String> currentItemsList = new ArrayList<>();
  currentItemsList.add("ITEM_ID"); // the current purchased item id;

  mSubscriptionProcessor.update(this, currentItemsList, "YOUR_ITEM_SKU",
          "DEVELOPER_PAYLOAD", new PurchaseHandler() {
              @Override
              public void onSuccess(Purchase purchase) {
                  // Do your sutff with the purchaded item
              }

              @Override
              public void onError(BillingException e) {
                  // Handle the error
              }
          }
  );
  ```
As a result you will get a list of [Purchase](#purchase-object) objects.

# Inventory of Purchases
* You can get the inventory of your purchases like this:

  ```java
  // For subscriptions
  mSubscriptionProcessor.getInventory(new InventoryHandler() {
      @Override
      public void onSuccess(PurchaseList purchaseList) {
          // Do your stuff with the list of purchases
      }

      @Override
      public void onError(BillingException e) {
          // Handle the error
      }
  });

  // For consumables/non-consumables
  mItemProcessor.getInventory(new InventoryHandler() {
      @Override
      public void onSuccess(PurchaseList purchaseList) {
          // Do your stuff with the list of purchases
      }

      @Override
      public void onError(BillingException e) {
          // Handle the error
      }
  });
  ```
As a result you will get a list of [Purchase](#purchase-object) objects.

# List of Items
* You can get a list of your item details such as prices and descriptions

  ```java
  ArrayList<String> itemIdsList = new ArrayList<>();

  // For subscriptions
  mSubscriptionProcessor.getItemList(itemIdsList, new ItemListHandler() {
      @Override
      public void onSuccess(ItemList itemList) {
          // Do your stuff with the list of items details
      }

      @Override
      public void onError(BillingException e) {
          // Handle the error
      }
  });

  // For consumables/non-consumables
  mItemProcessor.getItemList(itemIdsList, new ItemListHandler() {
      @Override
      public void onSuccess(ItemList itemList) {
          // Do your stuff with the list of items details
      }

      @Override
      public void onError(BillingException e) {
          // Handle the error
      }
  });
  ```
As a result you will get a list of [Item](#item-object) objects.

# Check In-App Billing service availability
* In some devices, In-App Billing may not be available.
Therefore, it is advisable to check whether it is available or not by calling `ItemProcessor.isServiceAvailable()` or `SubscriptionsProcessor.isServiceAvailable()` as follows:

  ```java
  boolean isAvailable = ItemProcessor.isServiceAvailable(getApplicationContext());
  if (!isAvailable) {
      // Abort
  }
  ```

# Purchase Object
* This object contains all the information of a purchase

  ```java
  public String getOriginalJson();
  public String getOrderId();
  public String getPackageName();
  public String getSku();
  public long getPurchaseTime();
  public int getPurchaseState();
  public String getDeveloperPayload();
  public String getToken();
  public boolean isAutoRenewing();
  public String getSignature();
  ```

# Item Object
* This object contains all the information of an item

  ```java
  public String getOriginalJson();
  public String getSku();
  public String getType();
  public String getTitle();
  public String getDescription();
  public String getCurrency();
  public String getPrice();
  public long getPriceMicros();
  ```