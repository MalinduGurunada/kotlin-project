import { readFile } from "node:fs/promises";
import { after, before, beforeEach, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  documentId,
  getDoc,
  getDocs,
  orderBy,
  query,
  runTransaction,
  setDoc,
  setLogLevel,
  serverTimestamp,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";

const PROJECT_ID = "demo-personal-finance-income-rules";
const FIRESTORE_HOST = "127.0.0.1";
const FIRESTORE_PORT = 8080;

setLogLevel("silent");

let testEnvironment;
let allowedAssertions = 0;
let deniedAssertions = 0;

before(async () => {
  const rules = await readFile(new URL("../firestore.rules", import.meta.url), "utf8");
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: FIRESTORE_HOST,
      port: FIRESTORE_PORT,
      rules,
    },
  });
});

beforeEach(async () => {
  await testEnvironment.clearFirestore();
});

after(async () => {
  await testEnvironment.cleanup();
  console.log(
    `RULE_ASSERTIONS allowed=${allowedAssertions} denied=${deniedAssertions} total=${allowedAssertions + deniedAssertions}`,
  );
});

async function expectAllowed(operation) {
  await assertSucceeds(operation);
  allowedAssertions += 1;
}

async function expectDenied(operation) {
  await assertFails(operation);
  deniedAssertions += 1;
}

function userFirestore(uid) {
  return testEnvironment.authenticatedContext(uid, { email_verified: true }).firestore();
}

function unverifiedUserFirestore(uid) {
  return testEnvironment.authenticatedContext(uid, { email_verified: false }).firestore();
}

function anonymousFirestore() {
  return testEnvironment.unauthenticatedContext().firestore();
}

function incomeReference(firestore, uid, incomeId) {
  return doc(firestore, "users", uid, "incomes", incomeId);
}

function expenseReference(firestore, uid, expenseId) {
  return doc(firestore, "users", uid, "expenses", expenseId);
}

function fcmTokenReference(firestore, uid, tokenId = "registration-hash") {
  return doc(firestore, "users", uid, "fcmRegistrations", tokenId);
}

function fcmTokenDocument(uid, overrides = {}) {
  return {
    schemaVersion: 1,
    userId: uid,
    installationId: "firebase-installation-id",
    platform: "android",
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function categoryReference(firestore, uid, categoryId) {
  return doc(firestore, "users", uid, "categories", categoryId);
}

function recurringReference(firestore, uid, recurringId) {
  return doc(firestore, "users", uid, "recurringExpenses", recurringId);
}

function occurrenceReference(firestore, uid, occurrenceId) {
  return doc(firestore, "users", uid, "recurringOccurrences", occurrenceId);
}

function receivableReference(firestore, uid, receivableId) {
  return doc(firestore, "users", uid, "receivables", receivableId);
}

function receivableDocument(receivableId, overrides = {}) {
  return {
    schemaVersion: 1,
    id: receivableId,
    title: "Website delivery",
    projectReference: "WEB-2026",
    expectedOriginalDecimal: "500.00",
    currency: "LKR",
    expectedDate: "2026-10-30",
    archived: false,
    ...overrides,
  };
}

function goalReference(firestore, uid, goalId) {
  return doc(firestore, "users", uid, "goals", goalId);
}

function contributionReference(firestore, uid, goalId, contributionId) {
  return doc(firestore, "users", uid, "goals", goalId, "contributions", contributionId);
}

function reversalClaimReference(firestore, uid, goalId, originalId) {
  return doc(firestore, "users", uid, "goals", goalId, "reversalClaims", originalId);
}

function goalDocument(goalId, overrides = {}) {
  return {
    schemaVersion: 1,
    id: goalId,
    name: "Emergency fund",
    targetAmountMinor: 1000000,
    startDate: "2026-10-01",
    targetDate: "2027-10-01",
    archived: false,
    ...overrides,
  };
}

function contributionDocument(goalId, contributionId, overrides = {}) {
  return {
    schemaVersion: 1,
    id: contributionId,
    goalId,
    amountMinor: 50000,
    kind: "ALLOCATION",
    occurredDate: "2026-10-20",
    ...overrides,
  };
}

function incomeDocument(incomeId, overrides = {}) {
  const data = {
    schemaVersion: 1,
    id: incomeId,
    source: "SALARY",
    entryKind: "RECEIPT",
    money: {
      originalAmountDecimal: "1250.50",
      originalCurrency: "LKR",
      rateLkrPerUnitDecimal: "1",
      baseAmountMinor: 125050,
      rateSource: "LKR_FIXED",
      rateDate: "2026-10-21",
    },
    occurredDate: "2026-10-21",
  };

  const { money, ...topLevel } = overrides;
  Object.assign(data, topLevel);
  if (money !== undefined) {
    data.money = money === null ? null : { ...data.money, ...money };
  }
  return data;
}

function usdIncome(incomeId, rateSource, overrides = {}) {
  return incomeDocument(incomeId, {
    source: "FREELANCE",
    money: {
      originalAmountDecimal: "125.00",
      originalCurrency: "USD",
      rateLkrPerUnitDecimal: "310.123456",
      baseAmountMinor: 3876543,
      rateSource,
      rateDate: "2026-10-18",
    },
    ...overrides,
  });
}

function cryptoIncome(incomeId, pnlDirection) {
  return incomeDocument(incomeId, {
    source: "CRYPTO",
    entryKind: "REALIZED_PNL",
    pnlDirection,
    assetSymbol: "BTC",
  });
}

function structuredCryptoIncome(incomeId, {
  proceeds = "1000.00",
  costBasis = "600.00",
  fees = "50.00",
  result = "350.00",
  resultMinor = 35000,
  direction = "GAIN",
} = {}) {
  return incomeDocument(incomeId, {
    source: "CRYPTO",
    entryKind: "REALIZED_PNL",
    pnlDirection: direction,
    assetSymbol: "BTC",
    money: {
      originalAmountDecimal: result,
      baseAmountMinor: resultMinor,
    },
    cryptoRealizedDetails: {
      costBasisOriginalDecimal: costBasis,
      saleProceedsOriginalDecimal: proceeds,
      feesOriginalDecimal: fees,
    },
  });
}

function expenseDocument(userId, expenseId, overrides = {}) {
  return {
    id: expenseId,
    userId,
    categoryId: "food",
    categoryNameSnapshot: "Food",
    spendingClass: "DISCRETIONARY",
    paymentChannel: "CASH",
    originalAmountDecimal: "1500.00",
    originalCurrency: "LKR",
    rateLkrPerUnitDecimal: "1",
    baseAmountMinor: 150000,
    rateSource: "LKR_FIXED",
    rateDate: "2026-10-21",
    occurredDate: "2026-10-21",
    note: null,
    channelLabel: null,
    recurringExpenseId: null,
    recurringOccurrenceId: null,
    ...overrides,
  };
}

function categoryDocument(categoryId) {
  return {
    id: categoryId,
    name: "Utilities & Bills",
    defaultSpendingClass: "COMMITTED",
    archived: false,
  };
}

function recurringDocument(userId, recurringId, overrides = {}) {
  return {
    id: recurringId,
    userId,
    title: "CEB Electricity Bill",
    categoryId: "utilities",
    expectedAmountMinor: 1250000,
    frequency: "MONTHLY",
    intervalCount: 1,
    anchorDate: "2026-10-31",
    startDate: "2026-10-31",
    endDate: null,
    status: "ACTIVE",
    paymentChannel: "BANK",
    channelLabel: "ComBank CASA",
    reminderEnabled: true,
    reminderLeadDays: 2,
    reminderTimeMinutes: 540,
    ...overrides,
  };
}

function occurrenceDocument(userId, recurringId, dueDate, status, overrides = {}) {
  const id = `${recurringId}_${dueDate}`;
  return {
    id,
    userId,
    recurringExpenseId: recurringId,
    dueDate,
    status,
    expenseId: status === "CONFIRMED" ? `recurring_${id}` : null,
    actionAt: Timestamp.fromDate(new Date("2026-10-30T08:00:00Z")),
    ...overrides,
  };
}

function withoutField(value, field) {
  const result = structuredClone(value);
  delete result[field];
  return result;
}

async function seedIncome(uid, incomeId, data = incomeDocument(incomeId)) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(incomeReference(context.firestore(), uid, incomeId), data);
  });
}

async function seedReceivable(uid, receivableId, data = receivableDocument(receivableId)) {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(receivableReference(context.firestore(), uid, receivableId), data);
  });
}

async function seedFcmRegistration(uid, tokenId = "registration-hash") {
  await testEnvironment.withSecurityRulesDisabled(async (context) => {
    await setDoc(
      fcmTokenReference(context.firestore(), uid, tokenId),
      fcmTokenDocument(uid),
    );
  });
}

function observerQuery(firestore, uid) {
  return query(
    collection(firestore, "users", uid, "incomes"),
    where("occurredDate", ">=", "2026-10-01"),
    where("occurredDate", "<", "2026-11-01"),
    orderBy("occurredDate", "asc"),
    orderBy(documentId(), "asc"),
  );
}

test("anonymous clients cannot read, query, create, update, or delete income", async () => {
  await seedIncome("user-a", "income-1");
  const firestore = anonymousFirestore();
  const reference = incomeReference(firestore, "user-a", "income-1");

  await expectDenied(getDoc(reference));
  await expectDenied(getDocs(observerQuery(firestore, "user-a")));
  await expectDenied(setDoc(incomeReference(firestore, "user-a", "income-2"), incomeDocument("income-2")));
  await expectDenied(updateDoc(reference, { note: "anonymous update" }));
  await expectDenied(deleteDoc(reference));
});

test("unverified owners can bootstrap their profile but cannot access financial data", async () => {
  await seedIncome("user-a", "income-1");
  await seedFcmRegistration("user-a");
  const firestore = unverifiedUserFirestore("user-a");

  await expectAllowed(setDoc(doc(firestore, "users", "user-a"), {
    displayName: "Pending Verification",
  }));
  await expectAllowed(getDoc(doc(firestore, "users", "user-a")));
  await expectDenied(setDoc(
    fcmTokenReference(firestore, "user-a", "new-registration"),
    fcmTokenDocument("user-a"),
  ));
  await expectDenied(updateDoc(
    fcmTokenReference(firestore, "user-a"),
    { platform: "android" },
  ));
  await expectDenied(getDoc(fcmTokenReference(firestore, "user-a")));
  await expectAllowed(deleteDoc(fcmTokenReference(firestore, "user-a")));
  await expectDenied(deleteDoc(fcmTokenReference(firestore, "user-b")));
  await expectDenied(getDoc(incomeReference(firestore, "user-a", "income-1")));
  await expectDenied(setDoc(
    incomeReference(firestore, "user-a", "income-2"),
    incomeDocument("income-2"),
  ));
  await expectDenied(setDoc(
    expenseReference(firestore, "user-a", "expense-1"),
    expenseDocument("user-a", "expense-1"),
  ));
  await expectDenied(setDoc(
    categoryReference(firestore, "user-a", "category-1"),
    categoryDocument("category-1"),
  ));
  await expectDenied(setDoc(
    receivableReference(firestore, "user-a", "receivable-1"),
    receivableDocument("receivable-1"),
  ));
  await expectDenied(setDoc(
    goalReference(firestore, "user-a", "goal-1"),
    goalDocument("goal-1"),
  ));
});

test("owners can create, read, list, update, and transactionally delete income", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");

  await expectAllowed(setDoc(reference, incomeDocument("income-1")));
  await expectAllowed(getDoc(reference));
  await expectAllowed(getDocs(collection(firestore, "users", "user-a", "incomes")));
  await expectAllowed(updateDoc(reference, { note: "owner update" }));
  await expectAllowed(
    runTransaction(firestore, async (transaction) => {
      const snapshot = await transaction.get(reference);
      if (!snapshot.exists()) {
        throw new Error("Expected seeded income");
      }
      transaction.delete(reference);
    }),
  );
});

test("one authenticated user cannot access another user's income collection", async () => {
  await seedIncome("user-b", "income-1");
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-b", "income-1");

  await expectDenied(getDoc(reference));
  await expectDenied(getDocs(collection(firestore, "users", "user-b", "incomes")));
  await expectDenied(setDoc(incomeReference(firestore, "user-b", "income-2"), incomeDocument("income-2")));
  await expectDenied(updateDoc(reference, { note: "cross-user update" }));
  await expectDenied(deleteDoc(reference));
});

test("schema version, exact top-level fields, required fields, and types are enforced", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const cases = [
    withoutField(incomeDocument("income-1"), "schemaVersion"),
    incomeDocument("income-1", { schemaVersion: 2 }),
    incomeDocument("income-1", { unexpected: true }),
    withoutField(incomeDocument("income-1"), "source"),
    incomeDocument("income-1", { occurredDate: 20261021 }),
    incomeDocument("different-id"),
    incomeDocument("income-1", { projectReference: 123 }),
    incomeDocument("income-1", { note: null }),
  ];

  for (const invalidDocument of cases) {
    await expectDenied(setDoc(reference, invalidDocument));
  }
});

test("nested Money requires its exact field set and supported Firestore types", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const missingRateDate = withoutField(incomeDocument("income-1").money, "rateDate");
  const missingNestedField = incomeDocument("income-1");
  missingNestedField.money = missingRateDate;
  const cases = [
    incomeDocument("income-1", { money: null }),
    missingNestedField,
    incomeDocument("income-1", { money: { unsupported: "field" } }),
    incomeDocument("income-1", { money: { originalAmountDecimal: 1250.5 } }),
    incomeDocument("income-1", { money: { baseAmountMinor: "125050" } }),
    incomeDocument("income-1", { money: { baseAmountMinor: 1250.5 } }),
    incomeDocument("income-1", { money: { rateDate: 20261021 } }),
  ];

  for (const invalidDocument of cases) {
    await expectDenied(setDoc(reference, invalidDocument));
  }
});

test("positive minor units and supported decimal string shapes are required", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const cases = [
    incomeDocument("income-1", { money: { baseAmountMinor: 0 } }),
    incomeDocument("income-1", { money: { baseAmountMinor: -1 } }),
    incomeDocument("income-1", { money: { originalAmountDecimal: "0.00" } }),
    incomeDocument("income-1", { money: { originalAmountDecimal: "1.234" } }),
    usdIncome("income-1", "MANUAL_TRANSFER"),
  ];
  cases[4].money.rateLkrPerUnitDecimal = "310.1234567";

  for (const invalidDocument of cases) {
    await expectDenied(setDoc(reference, invalidDocument));
  }
});

test("currency and rate-source combinations follow the domain contract", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const cases = [
    incomeDocument("income-1", { money: { originalCurrency: "EUR" } }),
    incomeDocument("income-1", { money: { rateSource: "MANUAL_TRANSFER" } }),
    incomeDocument("income-1", { money: { rateLkrPerUnitDecimal: "2" } }),
    incomeDocument("income-1", { money: { rateDate: "2026-10-20" } }),
    usdIncome("income-1", "LKR_FIXED"),
    usdIncome("income-1", "UNKNOWN_RATE_SOURCE"),
  ];

  for (const invalidDocument of cases) {
    await expectDenied(setDoc(reference, invalidDocument));
  }
});

test("precision-safe LKR unit-rate representations are accepted", async () => {
  const firestore = userFirestore("user-a");
  const validRates = ["1", "1.0", "1.00", "01", "10e-1", "1e0"];

  for (const [index, rate] of validRates.entries()) {
    const incomeId = `lkr-unit-rate-${index}`;
    await expectAllowed(
      setDoc(
        incomeReference(firestore, "user-a", incomeId),
        incomeDocument(incomeId, { money: { rateLkrPerUnitDecimal: rate } }),
      ),
    );
  }
});

test("near-one and invalid LKR rates are rejected on create and update", async () => {
  const firestore = userFirestore("user-a");
  const invalidRates = [
    "1.0000000000000001",
    "0.99999999999999999",
    "1.00000000000000001",
    "0.999999999999999999",
    "1.000001",
    "0.999999",
    "0",
    "-1",
    "not-a-rate",
    "1e-1",
    "1e309",
    "Infinity",
  ];

  for (const [index, rate] of invalidRates.entries()) {
    const incomeId = `invalid-lkr-rate-${index}`;
    await expectDenied(
      setDoc(
        incomeReference(firestore, "user-a", incomeId),
        incomeDocument(incomeId, { money: { rateLkrPerUnitDecimal: rate } }),
      ),
    );
  }

  const incomeId = "lkr-rate-update-target";
  const reference = incomeReference(firestore, "user-a", incomeId);
  await expectAllowed(setDoc(reference, incomeDocument(incomeId)));
  await expectDenied(
    setDoc(
      reference,
      incomeDocument(incomeId, {
        money: { rateLkrPerUnitDecimal: "1.0000000000000001" },
      }),
    ),
  );
  await expectDenied(
    updateDoc(reference, { "money.rateLkrPerUnitDecimal": "0.99999999999999999" }),
  );
  await expectDenied(
    setDoc(
      reference,
      { money: { rateLkrPerUnitDecimal: "1.000001" } },
      { merge: true },
    ),
  );

  const snapshot = await assertSucceeds(getDoc(reference));
  allowedAssertions += 1;
  if (snapshot.data().money.rateLkrPerUnitDecimal !== "1") {
    throw new Error("Rejected LKR rate update changed the persisted income document");
  }
});

test("income source, entry kind, and P&L combinations are enforced", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const cases = [
    incomeDocument("income-1", { source: "DIVIDEND" }),
    incomeDocument("income-1", { entryKind: "UNKNOWN" }),
    incomeDocument("income-1", { source: "CRYPTO", entryKind: "REALIZED_PNL" }),
    incomeDocument("income-1", { pnlDirection: "GAIN" }),
    cryptoIncome("income-1", "SIDEWAYS"),
    incomeDocument("income-1", { source: "CRYPTO", entryKind: "RECEIPT", pnlDirection: "GAIN" }),
  ];

  for (const invalidDocument of cases) {
    await expectDenied(setDoc(reference, invalidDocument));
  }
});

test("obviously malformed date strings are rejected", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");

  await expectDenied(setDoc(reference, incomeDocument("income-1", { occurredDate: "21-10-2026" })));
  await expectDenied(setDoc(reference, incomeDocument("income-1", { money: { rateDate: "2026/10/21" } })));
});

test("all supported income business cases are accepted", async () => {
  const firestore = userFirestore("user-a");
  await seedReceivable("user-a", "receivable-1");
  const validDocuments = [
    incomeDocument("salary"),
    incomeDocument("freelance", { source: "FREELANCE" }),
    incomeDocument("adsense", { source: "ADSENSE" }),
    cryptoIncome("crypto-gain", "GAIN"),
    cryptoIncome("crypto-loss", "LOSS"),
    structuredCryptoIncome("crypto-structured-gain"),
    structuredCryptoIncome("crypto-structured-loss", {
      proceeds: "500.00", costBasis: "600.00", fees: "25.00",
      result: "125.00", resultMinor: 12500, direction: "LOSS",
    }),
    structuredCryptoIncome("crypto-break-even", {
      proceeds: "600.00", costBasis: "600.00", fees: "0",
      result: "0", resultMinor: 0, direction: "BREAK_EVEN",
    }),
    incomeDocument("lkr-rate-scale", { money: { rateLkrPerUnitDecimal: "1.0" } }),
    incomeDocument("lkr-rate-scientific", { money: { rateLkrPerUnitDecimal: "10e-1" } }),
    usdIncome("usd-auto", "EXCHANGE_RATE_API_REFERENCE"),
    usdIncome("usd-transfer", "MANUAL_TRANSFER"),
    usdIncome("usd-reference", "MANUAL_REFERENCE"),
    incomeDocument("metadata", {
      source: "FREELANCE",
      projectReference: "project-1",
      receivableId: "receivable-1",
      assetSymbol: "LKR",
      note: "Complete mapper-compatible metadata",
    }),
  ];

  for (const validDocument of validDocuments) {
    await expectAllowed(
      setDoc(incomeReference(firestore, "user-a", validDocument.id), validDocument),
    );
  }
});

test("structured crypto schema permits zero break-even but rejects malformed or non-crypto details", async () => {
  const firestore = userFirestore("user-a");
  await expectAllowed(setDoc(
    incomeReference(firestore, "user-a", "break-even"),
    structuredCryptoIncome("break-even", {
      proceeds: "600", costBasis: "600", fees: "0",
      result: "0", resultMinor: 0, direction: "BREAK_EVEN",
    }),
  ));

  await expectDenied(setDoc(
    incomeReference(firestore, "user-a", "legacy-zero"),
    incomeDocument("legacy-zero", {
      source: "CRYPTO",
      entryKind: "REALIZED_PNL",
      pnlDirection: "GAIN",
      money: { originalAmountDecimal: "0", baseAmountMinor: 0 },
    }),
  ));

  const nonCrypto = incomeDocument("non-crypto-details", {
    cryptoRealizedDetails: structuredCryptoIncome("unused").cryptoRealizedDetails,
  });
  await expectDenied(setDoc(incomeReference(firestore, "user-a", "non-crypto-details"), nonCrypto));

  const malformed = structuredCryptoIncome("malformed");
  malformed.cryptoRealizedDetails.unexpected = "field";
  await expectDenied(setDoc(incomeReference(firestore, "user-a", "malformed"), malformed));
});

test("retrying a structured crypto save to the same owner and ID is idempotent", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "stable-id");
  const data = structuredCryptoIncome("stable-id");

  await expectAllowed(setDoc(reference, data));
  await expectAllowed(setDoc(reference, data));
  const snapshot = await assertSucceeds(getDoc(reference));
  allowedAssertions += 1;
  if (snapshot.data().money.baseAmountMinor !== 35000) {
    throw new Error("Retry changed the structured crypto result");
  }
});

test("full-document upserts can remove optional metadata", async () => {
  const firestore = userFirestore("user-a");
  await seedReceivable("user-a", "receivable-1");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const withMetadata = incomeDocument("income-1", {
    source: "FREELANCE",
    projectReference: "project-1",
    receivableId: "receivable-1",
    note: "Will be removed",
  });

  await expectAllowed(setDoc(reference, withMetadata));
  await expectAllowed(setDoc(reference, incomeDocument("income-1")));
  const snapshot = await assertSucceeds(getDoc(reference));
  allowedAssertions += 1;
  if ("projectReference" in snapshot.data() || "receivableId" in snapshot.data() || "note" in snapshot.data()) {
    throw new Error("Full-document set did not remove omitted optional metadata");
  }
});

test("receivables require authentication, exact schema, and owner-scoped paths", async () => {
  const owner = userFirestore("user-a");
  const other = userFirestore("user-b");
  const anonymous = anonymousFirestore();
  const ownerRef = receivableReference(owner, "user-a", "same-id");

  await expectDenied(setDoc(receivableReference(anonymous, "user-a", "same-id"), receivableDocument("same-id")));
  await expectAllowed(setDoc(ownerRef, receivableDocument("same-id")));
  await expectDenied(getDoc(receivableReference(other, "user-a", "same-id")));
  await expectDenied(setDoc(receivableReference(other, "user-a", "same-id"), receivableDocument("same-id")));
  await expectDenied(setDoc(receivableReference(owner, "user-a", "bad-id"), receivableDocument("different-id")));
  await expectDenied(setDoc(receivableReference(owner, "user-a", "extra"), {
    ...receivableDocument("extra"), unexpected: true,
  }));
  await expectDenied(setDoc(
    receivableReference(owner, "user-a", "float-version"),
    receivableDocument("float-version", { schemaVersion: 1.0 + Number.EPSILON }),
  ));

  await expectAllowed(setDoc(receivableReference(other, "user-b", "same-id"), receivableDocument("same-id", {
    title: "Other account's record",
  })));
  const ownerSnapshot = await assertSucceeds(getDoc(ownerRef));
  allowedAssertions += 1;
  if (ownerSnapshot.data().title !== "Website delivery") throw new Error("Identical IDs crossed account boundaries");
});

test("receivable links require an active same-account receivable with matching currency", async () => {
  const userA = userFirestore("user-a");
  await seedReceivable("user-a", "active-lkr");
  await seedReceivable("user-a", "archived-lkr", receivableDocument("archived-lkr", { archived: true }));
  await seedReceivable("user-b", "other-only");

  const linked = (incomeId, receivableId, overrides = {}) => incomeDocument(incomeId, {
    source: "FREELANCE", receivableId, ...overrides,
  });
  await expectAllowed(setDoc(incomeReference(userA, "user-a", "valid"), linked("valid", "active-lkr")));
  await expectDenied(setDoc(incomeReference(userA, "user-a", "missing"), linked("missing", "missing")));
  await expectDenied(setDoc(incomeReference(userA, "user-a", "cross-account"), linked("cross-account", "other-only")));
  await expectDenied(setDoc(incomeReference(userA, "user-a", "archived"), linked("archived", "archived-lkr")));
  await expectDenied(setDoc(incomeReference(userA, "user-a", "wrong-source"), incomeDocument("wrong-source", {
    source: "SALARY", receivableId: "active-lkr",
  })));
  await expectDenied(setDoc(incomeReference(userA, "user-a", "wrong-currency"), usdIncome("wrong-currency", "MANUAL_TRANSFER", {
    receivableId: "active-lkr",
  })));

  const receivable = receivableReference(userA, "user-a", "active-lkr");
  await expectAllowed(updateDoc(receivable, { expectedOriginalDecimal: "750.00" }));
  await expectDenied(updateDoc(receivable, { currency: "USD" }));
});

test("owner updates cannot bypass complete resulting-document validation", async () => {
  await seedIncome("user-a", "income-1");
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");

  await expectDenied(setDoc(reference, withoutField(incomeDocument("income-1"), "money")));
  await expectDenied(updateDoc(reference, { source: "DIVIDEND" }));
  await expectDenied(updateDoc(reference, { unexpected: true }));

  const snapshot = await assertSucceeds(getDoc(reference));
  allowedAssertions += 1;
  if (snapshot.data().source !== "SALARY" || "unexpected" in snapshot.data()) {
    throw new Error("Rejected update changed the persisted income document");
  }
});

test("the actual owner observer query is allowed and the same cross-user query is denied", async () => {
  await seedIncome("user-a", "income-1");
  await seedIncome("user-b", "income-2");
  const firestore = userFirestore("user-a");

  await expectAllowed(getDocs(observerQuery(firestore, "user-a")));
  await expectDenied(getDocs(observerQuery(firestore, "user-b")));
});

test("existing expense authorization remains owner-scoped", async () => {
  const ownerFirestore = userFirestore("user-a");
  const otherFirestore = userFirestore("user-b");
  const ownerReference = expenseReference(ownerFirestore, "user-a", "expense-1");

  await expectAllowed(
    setDoc(categoryReference(ownerFirestore, "user-a", "food"), categoryDocument("food")),
  );
  await expectAllowed(setDoc(ownerReference, expenseDocument("user-a", "expense-1")));
  await expectAllowed(getDoc(ownerReference));
  await expectDenied(getDoc(expenseReference(otherFirestore, "user-a", "expense-1")));
  await expectDenied(
    setDoc(
      expenseReference(otherFirestore, "user-a", "expense-2"),
      expenseDocument("user-a", "expense-2"),
    ),
  );
  await expectDenied(updateDoc(expenseReference(otherFirestore, "user-a", "expense-1"), { note: "denied" }));
  await expectDenied(deleteDoc(expenseReference(otherFirestore, "user-a", "expense-1")));
  await expectAllowed(deleteDoc(ownerReference));
});

test("expense writes require the exact financial schema and valid ownership", async () => {
  const firestore = userFirestore("user-a");
  await expectAllowed(
    setDoc(categoryReference(firestore, "user-a", "food"), categoryDocument("food")),
  );

  const validReference = expenseReference(firestore, "user-a", "valid-expense");
  await expectAllowed(
    setDoc(validReference, expenseDocument("user-a", "valid-expense")),
  );
  await expectAllowed(updateDoc(validReference, { note: "Edited offline-compatible note" }));
  await expectDenied(updateDoc(validReference, { userId: "user-b" }));
  await expectDenied(updateDoc(validReference, { id: "forged-id" }));
  await expectDenied(updateDoc(validReference, { unexpected: true }));
  await expectDenied(updateDoc(validReference, { baseAmountMinor: 0 }));

  const invalidDocuments = [
    withoutField(expenseDocument("user-a", "missing-note"), "note"),
    expenseDocument("user-a", "extra-field", { unexpected: true }),
    expenseDocument("user-b", "forged-owner"),
    expenseDocument("user-a", "zero-amount", { originalAmountDecimal: "0.00" }),
    expenseDocument("user-a", "numeric-amount", { originalAmountDecimal: 1500 }),
    expenseDocument("user-a", "zero-base", { baseAmountMinor: 0 }),
    expenseDocument("user-a", "bad-currency", { originalCurrency: "EUR" }),
    expenseDocument("user-a", "bad-rate", { rateLkrPerUnitDecimal: "1.25" }),
    expenseDocument("user-a", "bad-rate-source", { rateSource: "MANUAL_REFERENCE" }),
    expenseDocument("user-a", "bad-date", { occurredDate: "21-10-2026" }),
    expenseDocument("user-a", "blank-category-name", { categoryNameSnapshot: " " }),
    expenseDocument("user-a", "one-sided-link", { recurringExpenseId: "ceb" }),
  ];
  for (const invalid of invalidDocuments) {
    await expectDenied(
      setDoc(expenseReference(firestore, "user-a", invalid.id), invalid),
    );
  }
  await expectDenied(
    setDoc(
      expenseReference(firestore, "user-a", "wrong-id"),
      expenseDocument("user-a", "another-id"),
    ),
  );

  await expectDenied(
    setDoc(
      expenseReference(firestore, "user-a", "missing-category"),
      expenseDocument("user-a", "missing-category", { categoryId: "does-not-exist" }),
    ),
  );
});

test("Expense PaymentChannel values exactly match the Kotlin enum", async () => {
  const firestore = userFirestore("user-a");
  await expectAllowed(
    setDoc(categoryReference(firestore, "user-a", "food"), categoryDocument("food")),
  );

  for (const channel of ["CASH", "CARD", "BANK", "TRANSFER", "OTHER"]) {
    const expenseId = `channel-${channel.toLowerCase()}`;
    await expectAllowed(
      setDoc(
        expenseReference(firestore, "user-a", expenseId),
        expenseDocument("user-a", expenseId, { paymentChannel: channel }),
      ),
    );
  }
  for (const channel of ["WIRE", "BANK_TRANSFER", "transfer", ""]) {
    const expenseId = `invalid-channel-${channel || "blank"}`;
    await expectDenied(
      setDoc(
        expenseReference(firestore, "user-a", expenseId),
        expenseDocument("user-a", expenseId, { paymentChannel: channel }),
      ),
    );
  }
});

test("FCM tokens are owner-written, write-only, schema-checked, and cross-account isolated", async () => {
  const owner = userFirestore("user-a");
  const other = userFirestore("user-b");
  const anonymous = anonymousFirestore();
  const ownerReference = fcmTokenReference(owner, "user-a");

  await expectAllowed(setDoc(ownerReference, fcmTokenDocument("user-a")));
  await expectDenied(getDoc(ownerReference));
  await expectDenied(getDocs(collection(owner, "users", "user-a", "fcmRegistrations")));
  await expectDenied(setDoc(fcmTokenReference(other, "user-a"), fcmTokenDocument("user-a")));
  await expectDenied(setDoc(fcmTokenReference(anonymous, "user-a"), fcmTokenDocument("user-a")));
  await expectDenied(setDoc(
    fcmTokenReference(owner, "user-a", "forged-owner"),
    fcmTokenDocument("user-b"),
  ));
  await expectDenied(setDoc(
    fcmTokenReference(owner, "user-a", "extra-field"),
    fcmTokenDocument("user-a", { unexpected: true }),
  ));
  await expectAllowed(setDoc(ownerReference, fcmTokenDocument("user-a", {
    installationId: "refreshed-firebase-installation-id",
  })));
  await expectDenied(deleteDoc(fcmTokenReference(other, "user-a")));
  await expectAllowed(deleteDoc(ownerReference));
});

test("goal documents are owner-scoped, schema-checked, and not deletable", async () => {
  const owner = userFirestore("user-a");
  const other = userFirestore("user-b");
  const reference = goalReference(owner, "user-a", "goal-1");

  await expectAllowed(setDoc(reference, goalDocument("goal-1")));
  await expectAllowed(getDoc(reference));
  await expectAllowed(getDocs(collection(owner, "users", "user-a", "goals")));
  await expectAllowed(updateDoc(reference, { archived: true }));
  await expectDenied(deleteDoc(reference));
  await expectDenied(getDoc(goalReference(other, "user-a", "goal-1")));
  await expectDenied(setDoc(goalReference(other, "user-a", "goal-2"), goalDocument("goal-2")));

  const invalidGoals = [
    withoutField(goalDocument("invalid"), "schemaVersion"),
    goalDocument("invalid", { id: "different" }),
    goalDocument("invalid", { name: "" }),
    goalDocument("invalid", { targetAmountMinor: 0 }),
    goalDocument("invalid", { targetDate: "2025-01-01" }),
    goalDocument("invalid", { unexpected: true }),
  ];
  for (const invalid of invalidGoals) {
    await expectDenied(setDoc(goalReference(owner, "user-a", "invalid"), invalid));
  }
});

test("identical goal and contribution IDs remain isolated between owners", async () => {
  const userA = userFirestore("user-a");
  const userB = userFirestore("user-b");
  await expectAllowed(setDoc(goalReference(userA, "user-a", "same"), goalDocument("same", { name: "A" })));
  await expectAllowed(setDoc(goalReference(userB, "user-b", "same"), goalDocument("same", { name: "B" })));
  await expectAllowed(
    setDoc(
      contributionReference(userA, "user-a", "same", "same-entry"),
      contributionDocument("same", "same-entry", { amountMinor: 100 }),
    ),
  );
  await expectAllowed(
    setDoc(
      contributionReference(userB, "user-b", "same", "same-entry"),
      contributionDocument("same", "same-entry", { amountMinor: 250 }),
    ),
  );
  const aSnapshot = await assertSucceeds(
    getDoc(contributionReference(userA, "user-a", "same", "same-entry")),
  );
  allowedAssertions += 1;
  const bSnapshot = await assertSucceeds(
    getDoc(contributionReference(userB, "user-b", "same", "same-entry")),
  );
  allowedAssertions += 1;
  if (aSnapshot.data().amountMinor !== 100 || bSnapshot.data().amountMinor !== 250) {
    throw new Error("Identical contribution IDs crossed owner boundaries");
  }
  await expectDenied(getDoc(contributionReference(userA, "user-b", "same", "same-entry")));
});

test("contribution ledger is immutable and exact reversals require one atomic claim", async () => {
  const firestore = userFirestore("user-a");
  const goalId = "goal-1";
  await expectAllowed(setDoc(goalReference(firestore, "user-a", goalId), goalDocument(goalId)));
  const original = contributionReference(firestore, "user-a", goalId, "original");
  await expectAllowed(setDoc(original, contributionDocument(goalId, "original")));
  await expectDenied(updateDoc(original, { amountMinor: 60000 }));
  await expectDenied(deleteDoc(original));

  const reversal = contributionReference(firestore, "user-a", goalId, "reversal");
  await expectDenied(
    setDoc(
      reversal,
      contributionDocument(goalId, "reversal", {
        amountMinor: -50000,
        kind: "REVERSAL",
        reversesId: "original",
      }),
    ),
  );

  const batch = writeBatch(firestore);
  batch.set(
    reversalClaimReference(firestore, "user-a", goalId, "original"),
    {
      schemaVersion: 1,
      originalContributionId: "original",
      reversalContributionId: "reversal",
    },
  );
  batch.set(
    reversal,
    contributionDocument(goalId, "reversal", {
      amountMinor: -50000,
      kind: "REVERSAL",
      reversesId: "original",
    }),
  );
  await expectAllowed(batch.commit());

  const duplicateBatch = writeBatch(firestore);
  duplicateBatch.set(
    reversalClaimReference(firestore, "user-a", goalId, "original"),
    {
      schemaVersion: 1,
      originalContributionId: "original",
      reversalContributionId: "duplicate",
    },
  );
  duplicateBatch.set(
    contributionReference(firestore, "user-a", goalId, "duplicate"),
    contributionDocument(goalId, "duplicate", {
      amountMinor: -50000,
      kind: "REVERSAL",
      reversesId: "original",
    }),
  );
  await expectDenied(duplicateBatch.commit());
});

test("recurring commitments are owner scoped, validated, and never deleted", async () => {
  const owner = userFirestore("user-a");
  const other = userFirestore("user-b");
  await expectAllowed(
    setDoc(categoryReference(owner, "user-a", "utilities"), categoryDocument("utilities")),
  );
  await expectAllowed(
    setDoc(
      recurringReference(owner, "user-a", "ceb"),
      recurringDocument("user-a", "ceb"),
    ),
  );
  await expectDenied(getDoc(recurringReference(other, "user-a", "ceb")));
  await expectDenied(deleteDoc(recurringReference(owner, "user-a", "ceb")));
  await expectDenied(
    setDoc(
      recurringReference(owner, "user-a", "invalid"),
      recurringDocument("user-a", "invalid", { intervalCount: 0 }),
    ),
  );
  await expectDenied(
    setDoc(
      recurringReference(owner, "user-a", "invalid-time"),
      recurringDocument("user-a", "invalid-time", { reminderTimeMinutes: 1440 }),
    ),
  );
  await expectAllowed(
    setDoc(
      recurringReference(owner, "user-a", "legacy-time"),
      withoutField(recurringDocument("user-a", "legacy-time"), "reminderTimeMinutes"),
    ),
  );
  await expectAllowed(
    setDoc(
      recurringReference(owner, "user-a", "transfer-template"),
      recurringDocument("user-a", "transfer-template", { paymentChannel: "TRANSFER" }),
    ),
  );
  await expectDenied(
    setDoc(
      recurringReference(owner, "user-a", "invalid-channel"),
      recurringDocument("user-a", "invalid-channel", { paymentChannel: "WIRE" }),
    ),
  );
});

test("skipped recurring occurrence is immutable and uses a deterministic id", async () => {
  const firestore = userFirestore("user-a");
  await expectAllowed(
    setDoc(categoryReference(firestore, "user-a", "utilities"), categoryDocument("utilities")),
  );
  await expectAllowed(
    setDoc(
      recurringReference(firestore, "user-a", "ceb"),
      recurringDocument("user-a", "ceb"),
    ),
  );
  const occurrenceId = "ceb_2026-10-31";
  await expectAllowed(
    setDoc(
      occurrenceReference(firestore, "user-a", occurrenceId),
      occurrenceDocument("user-a", "ceb", "2026-10-31", "SKIPPED"),
    ),
  );
  await expectDenied(
    updateDoc(occurrenceReference(firestore, "user-a", occurrenceId), {
      status: "CONFIRMED",
      expenseId: `recurring_${occurrenceId}`,
    }),
  );
  await expectDenied(
    setDoc(
      occurrenceReference(firestore, "user-a", "made-up-id"),
      occurrenceDocument("user-a", "ceb", "2026-11-30", "SKIPPED", {
        id: "made-up-id",
      }),
    ),
  );
});

test("confirmed occurrence requires a consistent linked expense and remains intact", async () => {
  const firestore = userFirestore("user-a");
  await expectAllowed(
    setDoc(categoryReference(firestore, "user-a", "utilities"), categoryDocument("utilities")),
  );
  await expectAllowed(
    setDoc(
      recurringReference(firestore, "user-a", "ceb"),
      recurringDocument("user-a", "ceb"),
    ),
  );
  const occurrenceId = "ceb_2026-10-31";
  const expenseId = `recurring_${occurrenceId}`;
  const confirmed = occurrenceDocument("user-a", "ceb", "2026-10-31", "CONFIRMED");
  await expectDenied(
    setDoc(occurrenceReference(firestore, "user-a", occurrenceId), confirmed),
  );

  const batch = writeBatch(firestore);
  batch.set(
    expenseReference(firestore, "user-a", expenseId),
    {
      ...expenseDocument("user-a", expenseId),
      categoryId: "utilities",
      categoryNameSnapshot: "Utilities & Bills",
      recurringExpenseId: "ceb",
      recurringOccurrenceId: occurrenceId,
    },
  );
  batch.set(occurrenceReference(firestore, "user-a", occurrenceId), confirmed);
  await expectAllowed(batch.commit());

  const linkedExpense = expenseReference(firestore, "user-a", expenseId);
  await expectAllowed(updateDoc(linkedExpense, {
    note: "Corrected description",
    paymentChannel: "TRANSFER",
  }));
  await expectDenied(updateDoc(linkedExpense, {
    recurringExpenseId: null,
    recurringOccurrenceId: null,
  }));
  await expectDenied(updateDoc(linkedExpense, { recurringExpenseId: "forged-template" }));
  await expectDenied(updateDoc(linkedExpense, { recurringOccurrenceId: "ceb_2026-11-30" }));
  await expectDenied(deleteDoc(linkedExpense));
  await expectDenied(deleteDoc(occurrenceReference(firestore, "user-a", occurrenceId)));

  await expectDenied(
    setDoc(
      occurrenceReference(firestore, "user-a", "ceb_2026-11-30"),
      occurrenceDocument("user-a", "ceb", "2026-11-30", "CONFIRMED", {
        expenseId,
      }),
    ),
  );

  const inconsistentOccurrenceId = "ceb_2027-01-31";
  const inconsistentExpenseId = `recurring_${inconsistentOccurrenceId}`;
  await expectAllowed(
    setDoc(
      recurringReference(firestore, "user-a", "different-template"),
      recurringDocument("user-a", "different-template"),
    ),
  );
  const inconsistentBatch = writeBatch(firestore);
  inconsistentBatch.set(
    expenseReference(firestore, "user-a", inconsistentExpenseId),
    expenseDocument("user-a", inconsistentExpenseId, {
      categoryId: "utilities",
      categoryNameSnapshot: "Utilities & Bills",
      recurringExpenseId: "different-template",
      recurringOccurrenceId: inconsistentOccurrenceId,
    }),
  );
  inconsistentBatch.set(
    occurrenceReference(firestore, "user-a", inconsistentOccurrenceId),
    occurrenceDocument("user-a", "ceb", "2027-01-31", "CONFIRMED"),
  );
  await expectDenied(inconsistentBatch.commit());
});

test("unrelated paths and broader cross-user access remain denied", async () => {
  const firestore = userFirestore("user-a");

  await expectDenied(getDoc(doc(firestore, "unrelated", "document")));
  await expectDenied(setDoc(doc(firestore, "unrelated", "document"), { value: true }));
  await expectDenied(getDocs(collection(firestore, "users", "user-b", "incomes")));
});
