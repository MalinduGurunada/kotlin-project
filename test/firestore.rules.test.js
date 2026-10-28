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
  updateDoc,
  where,
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
  return testEnvironment.authenticatedContext(uid).firestore();
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

function usdIncome(incomeId, rateSource) {
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

function expenseDocument(userId, expenseId) {
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
  const validDocuments = [
    incomeDocument("salary"),
    incomeDocument("freelance", { source: "FREELANCE" }),
    incomeDocument("adsense", { source: "ADSENSE" }),
    cryptoIncome("crypto-gain", "GAIN"),
    cryptoIncome("crypto-loss", "LOSS"),
    incomeDocument("lkr-rate-scale", { money: { rateLkrPerUnitDecimal: "1.0" } }),
    incomeDocument("lkr-rate-scientific", { money: { rateLkrPerUnitDecimal: "10e-1" } }),
    usdIncome("usd-auto", "EXCHANGE_RATE_API_REFERENCE"),
    usdIncome("usd-transfer", "MANUAL_TRANSFER"),
    usdIncome("usd-reference", "MANUAL_REFERENCE"),
    incomeDocument("metadata", {
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

test("full-document upserts can remove optional metadata", async () => {
  const firestore = userFirestore("user-a");
  const reference = incomeReference(firestore, "user-a", "income-1");
  const withMetadata = incomeDocument("income-1", {
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

test("unrelated paths and broader cross-user access remain denied", async () => {
  const firestore = userFirestore("user-a");

  await expectDenied(getDoc(doc(firestore, "unrelated", "document")));
  await expectDenied(setDoc(doc(firestore, "unrelated", "document"), { value: true }));
  await expectDenied(getDocs(collection(firestore, "users", "user-b", "incomes")));
});
