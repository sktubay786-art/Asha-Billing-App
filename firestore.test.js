const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validCustomer(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Ramesh Mondal",
    phone: "9876543210",
    village: "Raghunathpur",
    address: "Ghatal, Paschim Medinipur",
    openingDue: 500,
    createdAt: now,
    updatedAt: now,
  };
}

function validBill(uid, customerId) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    billNo: "ASHA-2026-0001",
    date: "2026-11-18",
    customerId,
    customerName: "Ramesh Mondal",
    phone: "9876543210",
    address: "Ghatal, Paschim Medinipur",
    village: "Raghunathpur",
    season: "Aman season",
    landAmount: 20,
    unit: "decimal",
    bigha: 0.5,
    rate: 1200,
    current: 600,
    previousDue: 500,
    allTotal: 1100,
    note: "First irrigation",
    createdAt: now,
    updatedAt: now,
  };
}

function validPayment(uid, billId, customerId) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    billId,
    customerId,
    date: "2026-11-18",
    amount: 400,
    mode: "Cash",
    receivedIn: "Cash",
    note: "Initial payment",
    createdAt: now,
    updatedAt: now,
  };
}

function validExpense(uid) {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    title: "Diesel 20L",
    category: "Fuel",
    amount: 1900,
    date: "2026-11-18",
    paidMode: "Cash",
    note: "Pump #1 fuel",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/bills`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/payments`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/expenses`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/settings`).doc("profile").get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomer(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/expenses`).doc("exp_1").set(validExpense(ALICE_UID))
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").set(validCustomer(ALICE_UID))
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").update({ name: "Hijacked" })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1").delete());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/customers`).doc("cust_2").set(validCustomer(BOB_UID))
  );
});

test("Owner settings: create, partial update, and ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const ref = aliceDb.collection(`users/${ALICE_UID}/settings`).doc("profile");

  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      company: "ASHA Mini Shallow",
      owner: "SK Ekramul Haque",
      contact: "9564061920",
      address: "Raghunathpur, Ghatal",
      upi: "8710065540@axl",
      payee: "SK ENAMUL HAQUE",
      country: "+91",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    ref.update({
      company: "ASHA Billing",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    ref.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    ref.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Customers, Bills, and Payments: batch create, relational checks, partial update, and invalid payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Reject customer with extra @DocumentId 'id' field
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_bad").set({
      id: "cust_bad",
      ...validCustomer(ALICE_UID),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_future").set({
      ...validCustomer(ALICE_UID),
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Valid customer create
  const custRef = aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_1");
  await assertSucceeds(custRef.set(validCustomer(ALICE_UID)));

  // Valid customer partial update
  await assertSucceeds(
    custRef.update({
      openingDue: 250,
      landArea: "2.5 Bigha",
      landOwnerType: "Other",
      landOwnerName: "Sujit Maity",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Invalid landOwnerType must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/customers`).doc("cust_bad_owner").set({
      ...validCustomer(ALICE_UID),
      landArea: "1 Bigha",
      landOwnerType: "Rented",
      landOwnerName: "Someone",
    })
  );

  // Bill referencing non-existent customer must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/bills`).doc("bill_orphan").set(validBill(ALICE_UID, "missing_cust"))
  );

  // Atomic batch: bill + initial payment succeeds via existsAfter()
  const batch = aliceDb.batch();
  const billRef = aliceDb.collection(`users/${ALICE_UID}/bills`).doc("bill_1");
  const payRef = aliceDb.collection(`users/${ALICE_UID}/payments`).doc("pay_1");
  batch.set(billRef, validBill(ALICE_UID, "cust_1"));
  batch.set(payRef, validPayment(ALICE_UID, "bill_1", "cust_1"));
  await assertSucceeds(batch.commit());

  // Payment referencing non-existent bill must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/payments`).doc("pay_bad").set(validPayment(ALICE_UID, "missing_bill", "cust_1"))
  );

  // Payment with invalid mode must fail
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/payments`).doc("pay_mode").set({
      ...validPayment(ALICE_UID, "bill_1", "cust_1"),
      mode: "Crypto",
    })
  );

  // List queries succeed for owner
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/customers`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/bills`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/payments`).get());
});

test("Shared owner workspace: authorized emails and phones share data, unverified/outsiders are denied", async () => {
  const SHARED = "asha_owner_shared";
  const email1Db = testEnv.authenticatedContext("uid_email_1", {
    email: "sktubay786@gmail.com",
    email_verified: true,
  }).firestore();
  const email2Db = testEnv.authenticatedContext("uid_email_2", {
    email: "GamingTubay@gmail.com",
    email_verified: true,
  }).firestore();
  const phone1Db = testEnv.authenticatedContext("uid_phone_1", {
    phone_number: "+918710065540",
  }).firestore();
  const phone2Db = testEnv.authenticatedContext("uid_phone_2", {
    phone_number: "+919564061920",
  }).firestore();
  const unverifiedDb = testEnv.authenticatedContext("uid_unverified", {
    email: "sktubay786@gmail.com",
    email_verified: false,
  }).firestore();
  const outsiderDb = testEnv.authenticatedContext("uid_outsider", {
    email: "random@gmail.com",
    email_verified: true,
  }).firestore();

  // First authorized owner writes customer and settings into shared workspace
  const custRef = email1Db.collection(`users/${SHARED}/customers`).doc("cust_shared_1");
  await assertSucceeds(custRef.set(validCustomer(SHARED)));
  await assertSucceeds(
    phone1Db.collection(`users/${SHARED}/settings`).doc("profile").set({
      userId: SHARED,
      company: "আশা মিনি শ্যালো",
      owner: "SK EKRAMUL HAQUE",
      contact: "9564061920",
      address: "Raghunathpur, Chaklachipur, Ghatal, Paschim Medinipur, 721232",
      upi: "8710065540@axl",
      payee: "SK ENAMUL HAQUE",
      country: "+91",
      createdAt: new Date(Date.now() - 3000),
      updatedAt: new Date(Date.now() - 3000),
    })
  );

  // Second authorized owner email and phone read the same records and write bills
  await assertSucceeds(email2Db.collection(`users/${SHARED}/customers`).get());
  await assertSucceeds(phone2Db.collection(`users/${SHARED}/settings`).doc("profile").get());
  await assertSucceeds(
    phone2Db.collection(`users/${SHARED}/bills`).doc("bill_shared_1").set(validBill(SHARED, "cust_shared_1"))
  );

  // Unverified email and non-owner users are denied access to shared workspace
  await assertFails(unverifiedDb.collection(`users/${SHARED}/customers`).get());
  await assertFails(outsiderDb.collection(`users/${SHARED}/customers`).get());
  await assertFails(
    outsiderDb.collection(`users/${SHARED}/customers`).doc("cust_hack").set(validCustomer(SHARED))
  );
});

test("Expenses: owner create, partial update, delete, shared access, and validation rejections", async () => {
  const SHARED = "asha_owner_shared";
  const owner1Db = testEnv.authenticatedContext("uid_owner_1", {
    email: "sktubay786@gmail.com",
    email_verified: true,
  }).firestore();
  const owner2Db = testEnv.authenticatedContext("uid_owner_2", {
    email: "gamingtubay@gmail.com",
    email_verified: true,
  }).firestore();
  const outsiderDb = testEnv.authenticatedContext("uid_outsider_exp", {
    email: "outsider@gmail.com",
    email_verified: true,
  }).firestore();

  const expRef = owner1Db.collection(`users/${SHARED}/expenses`).doc("exp_1");
  await assertSucceeds(expRef.set(validExpense(SHARED)));

  // Second owner reads and updates same expense
  await assertSucceeds(owner2Db.collection(`users/${SHARED}/expenses`).get());
  await assertSucceeds(
    owner2Db.collection(`users/${SHARED}/expenses`).doc("exp_1").update({
      amount: 2100,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative/zero amount, invalid category, ghost field, and outsider access
  await assertFails(
    owner1Db.collection(`users/${SHARED}/expenses`).doc("exp_bad_amt").set({
      ...validExpense(SHARED),
      amount: 0,
    })
  );
  await assertFails(
    owner1Db.collection(`users/${SHARED}/expenses`).doc("exp_bad_cat").set({
      ...validExpense(SHARED),
      category: "Personal",
    })
  );
  await assertFails(
    owner1Db.collection(`users/${SHARED}/expenses`).doc("exp_1").update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(outsiderDb.collection(`users/${SHARED}/expenses`).get());

  // Owner can delete expense
  await assertSucceeds(expRef.delete());
});

