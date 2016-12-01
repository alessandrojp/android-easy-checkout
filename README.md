# Android In-App Billing Library
[![Build Status](https://travis-ci.org/alessandrojp/android-inapp-billing.svg)](https://travis-ci.org/alessandrojp/android-inapp-billing)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Fast and easy to use Android In-app Billing Library.

This library supports both non-consumable/consumable items and upgrading/downgrading of subscriptions. 

Also it supports [RxJava](https://github.com/alessandrojp/android-inapp-billing/).

NOTE: Upgrade/Downgrade of subscriptions are only available in the api version 5.
You can still set the api version 3 for other actions in the library.
The api version 5 automatically will be used only in this case.

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
  ```

### Creating a Billing Context
* You need a **BillingContext** object to execute actions in this library.
You can create one as follows:

  ```java
  // Application context
  Context context = getApplicationContext();

  // Public key generated on the Google Play Console
  String base64EncodedPublicKey = "YOUR_PUBLIC_KEY";

  BillingContext context = new BillingContext(
                context,
                base64EncodedPublicKey,
                BillingApi.VERSION_3, // It also supports api version 5
                new SystemLogger() // If don't want to check the logs, you can just give null
  ```

* See the sample of how to use it:

  ```java
  public class SampleActivity extends Activity {
    private static final int PURCHASE_REQUEST_CODE = 16; // Use any request code you want here
    private static final String ITEM_ID = "YOUR_ITEM_ID";

    private final PurchaseHandler mPurchaseHandler = new PurchaseHandler() {
      @Override
      public void call(PurchaseResponse response) {
          if (response.isSuccess()) {
              Purchase purchase = response.getPurchase();
              // Do your stuff with the purchased item
          } else {
              // Handle the error
          }
      }
    };
    private BillingProcessor mBillingProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      initBillingProcessor();
    }

    private void initBillingProcessor() {
      // Your public key
      String base64EncodedPublicKey = "YOUR_PUBLIC_KEY";

      BillingContext context = new BillingContext(
              getApplicationContext(), // App context
              base64EncodedPublicKey, // Public key generated on the Google Play Console
              BillingApi.VERSION_3, // It also supports version 5
              new SystemLogger() // If don't want to check the logs, you can just send null
      );
      mBillingProcessor = new BillingProcessor(context, mPurchaseHandler);
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      mBillingProcessor.release();
    }
  }
  ```

* Don't forget to override Activity's onActivityResult method like this:

  ```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (mBillingProcessor.onActivityResult(requestCode, resultCode, data)) {
          // The response will be sent through PurchaseHandler
          return;
      }
      super.onActivityResult(requestCode, resultCode, data);
    }
  ```

# Purchase Item
* Call `BillingProcessor#startPurchase` method to purchase a consumable/non-consumable or a subscription item  like this:

  ```java
  String developerPayload = "YOUR_DEVELOPER_PAYLOAD";
  
  mBillingProcessor.startPurchase(
                  this, // Activity
                  PURCHASE_REQUEST_CODE,
                  ITEM_ID,
                  PurchaseType.IN_APP, // or PurchaseType.SUBSCRIPTION for subscriptions
                  developerPayload,
                  new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                      // Billing activity started successfully
                      // Do nothing
                    }

                    @Override
                    public void onError(BillingException e) {
                      // Handle the error
                    }
                  });
  ```
As a result you will get a [Purchase](#purchase-object) object.

# Consume Purchased Item
* You can consume a purchased item anytime and allow the user to buy the same item again. To do this call `BillingProcessor#consume` like this:

  ```java
  mBillingProcessor.consume(ITEM_ID, new ConsumeItemHandler() {
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
* You can upgrade/downgrade an existing subscription by calling `BillingProcessor#updateSubscription` like this:

**This is only available in the api version 5.
You can still set the api version 3 for other actions in the library.
The api version 5 automatically will be used only in this case.**

  ```java
  ArrayList<String> currentItemsList = new ArrayList<>();
  currentItemIdList.add("ITEM_ID"); // the current purchased item id;
  String targetItemId = "TARGET_ITEM_ID";
  String developerPayload = "YOUR_DEVELOPER_PAYLOAD";

  mBillingProcessor.updateSubscription(
                  this, // Activity
                  PURCHASE_REQUEST_CODE,
                  currentItemIdList,
                  targetItemId,
                  developerPayload, 
                  new StartActivityHandler() {
                    @Override
                    public void onSuccess() {
                      // Billing activity started successfully
                      // Do nothing
                    }

                    @Override
                    public void onError(BillingException e) {
                      // Handle the error
                    }
                  });
  ```
As a result you will get a list of [Purchase](#purchase-object) objects.

# Inventory of Purchases
* You can get the inventory of your purchases like this:

  ```java
  mBillingProcessor.getInventory(
    PurchaseType.IN_APP, // PurchaseType.SUBSCRIPTION for subscriptions
    new InventoryHandler() {
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

# List of Item details
* You can get a list of your sku item details such as prices and descriptions

  ```java
  ArrayList<String> itemIdList = new ArrayList<>();

  mBillingProcessor.getItemDetailList(
    PurchaseType.IN_APP, // PurchaseType.SUBSCRIPTION for subscriptions
    itemIdList,
    new ItemDetailListHandler() {
      @Override
      public void onSuccess(ItemDetailList itemDetailList) {
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
Therefore, it is advisable to check whether it is available or not by calling `BillingProcessor.isServiceAvailable` as follows:

  ```java
  boolean isAvailable = BillingProcessor.isServiceAvailable(getApplicationContext());
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