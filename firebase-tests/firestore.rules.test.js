const { after, afterEach, before, test } = require("node:test");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");

const {
    assertFails,
    assertSucceeds,
    initializeTestEnvironment
} = require("@firebase/rules-unit-testing");

const {
    collection,
    deleteDoc,
    doc,
    getDoc,
    getDocs,
    setDoc,
    updateDoc
} = require("firebase/firestore");

const PROJECT_ID = "demo-clearledger";
const ALICE = "alice";
const BOB = "bob";

const DIRECT_COLLECTIONS = [
    "incomeSources",
    "incomeEntries",
    "projects",
    "cryptoTrades",
    "categories",
    "expenses",
    "recurring",
    "goals",
    "checkins",
    "freelanceProjects",
    "cryptoEntries",
    "cryptoHoldings",
    "savingsGoals"
];

let testEnv;

function aliceDb() {
    return testEnv.authenticatedContext(ALICE).firestore();
}

function bobDb() {
    return testEnv.authenticatedContext(BOB).firestore();
}

function signedOutDb() {
    return testEnv.unauthenticatedContext().firestore();
}

async function seedDocument(path, data = { value: 1 }) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
        await setDoc(doc(context.firestore(), path), data);
    });
}

async function seedDirectCollectionDocuments() {
    await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();

        for (const collectionName of DIRECT_COLLECTIONS) {
            await setDoc(
                doc(
                    db,
                    "users",
                    ALICE,
                    collectionName,
                    `${collectionName}-1`
                ),
                { value: 1 }
            );
        }
    });
}

before(async () => {
    const rulesPath = resolve(__dirname, "..", "firestore.rules");

    testEnv = await initializeTestEnvironment({
        projectId: PROJECT_ID,
        firestore: {
            rules: readFileSync(rulesPath, "utf8")
        }
    });

    await testEnv.clearFirestore();
});

afterEach(async () => {
    if (testEnv) {
        await testEnv.clearFirestore();
    }
});

after(async () => {
    if (testEnv) {
        await testEnv.cleanup();
    }
});

test("profile owner can create, read, update and delete their profile", async () => {
    const db = aliceDb();
    const profile = doc(db, "users", ALICE);

    await assertSucceeds(
        setDoc(profile, {
            uid: ALICE,
            displayName: "Alice",
            baseCurrency: "LKR"
        })
    );

    await assertSucceeds(getDoc(profile));

    await assertSucceeds(
        updateDoc(profile, {
            displayName: "Alice Updated"
        })
    );

    await assertSucceeds(deleteDoc(profile));
});

test("profile create and update require the stored UID to match the owner", async () => {
    const db = aliceDb();
    const profile = doc(db, "users", ALICE);

    await assertFails(
        setDoc(profile, {
            uid: BOB,
            displayName: "Invalid"
        })
    );

    await assertFails(
        setDoc(profile, {
            displayName: "Missing UID"
        })
    );

    await seedDocument(`users/${ALICE}`, {
        uid: ALICE,
        displayName: "Alice"
    });

    await assertFails(
        updateDoc(profile, {
            uid: BOB
        })
    );
});

test("one user cannot access another user's profile", async () => {
    const profile = doc(bobDb(), "users", ALICE);

    await assertFails(
        setDoc(profile, { uid: ALICE, displayName: "Created by Bob" })
    );

    await seedDocument(`users/${ALICE}`, {
        uid: ALICE,
        displayName: "Alice"
    });

    await assertFails(getDoc(profile));

    await assertFails(
        setDoc(profile, {
            uid: ALICE,
            displayName: "Changed by Bob"
        })
    );

    await assertFails(
        updateDoc(profile, {
            displayName: "Changed by Bob"
        })
    );

    await assertFails(deleteDoc(profile));
});

test("signed-out clients cannot access user profiles", async () => {
    const profile = doc(signedOutDb(), "users", ALICE);

    await assertFails(
        setDoc(profile, { uid: ALICE, displayName: "Signed-out create" })
    );

    await seedDocument(`users/${ALICE}`, {
        uid: ALICE,
        displayName: "Alice"
    });

    await assertFails(getDoc(profile));

    await assertFails(
        setDoc(profile, {
            uid: ALICE,
            displayName: "Signed-out write"
        })
    );

    await assertFails(
        updateDoc(profile, {
            displayName: "Signed-out update"
        })
    );

    await assertFails(deleteDoc(profile));
});

test("owners can access every approved direct financial collection", async () => {
    const db = aliceDb();

    for (const collectionName of DIRECT_COLLECTIONS) {
        const documentRef = doc(
            db,
            "users",
            ALICE,
            collectionName,
            `${collectionName}-1`
        );

        await assertSucceeds(
            setDoc(documentRef, {
                value: 1,
                collectionName
            })
        );

        await assertSucceeds(getDoc(documentRef));

        await assertSucceeds(
            updateDoc(documentRef, {
                value: 2
            })
        );

        await assertSucceeds(
            getDocs(
                collection(
                    db,
                    "users",
                    ALICE,
                    collectionName
                )
            )
        );

        await assertSucceeds(deleteDoc(documentRef));
    }
});

test("cross-user access is denied for every approved collection", async () => {
    await seedDirectCollectionDocuments();

    const db = bobDb();

    for (const collectionName of DIRECT_COLLECTIONS) {
        const existingDocument = doc(
            db,
            "users",
            ALICE,
            collectionName,
            `${collectionName}-1`
        );

        const newDocument = doc(
            db,
            "users",
            ALICE,
            collectionName,
            `${collectionName}-2`
        );

        await assertFails(getDoc(existingDocument));
        await assertFails(setDoc(newDocument, { value: 1 }));
        await assertFails(updateDoc(existingDocument, { value: 2 }));
        await assertFails(deleteDoc(existingDocument));

        await assertFails(
            getDocs(
                collection(
                    db,
                    "users",
                    ALICE,
                    collectionName
                )
            )
        );
    }
});

test("signed-out access is denied for every approved collection", async () => {
    await seedDirectCollectionDocuments();

    const db = signedOutDb();

    for (const collectionName of DIRECT_COLLECTIONS) {
        const existingDocument = doc(
            db,
            "users",
            ALICE,
            collectionName,
            `${collectionName}-1`
        );

        const newDocument = doc(
            db,
            "users",
            ALICE,
            collectionName,
            `${collectionName}-2`
        );

        await assertFails(getDoc(existingDocument));
        await assertFails(setDoc(newDocument, { value: 1 }));
        await assertFails(updateDoc(existingDocument, { value: 2 }));
        await assertFails(deleteDoc(existingDocument));

        await assertFails(
            getDocs(
                collection(
                    db,
                    "users",
                    ALICE,
                    collectionName
                )
            )
        );
    }
});

test("only the owner can access nested savings-goal contributions", async () => {
    const contributionsPath = `users/${ALICE}/savingsGoals/goal-1/contributions`;
    const contributionPath = `${contributionsPath}/contribution-1`;

    const ownerContribution = doc(aliceDb(), contributionPath);

    await assertSucceeds(
        setDoc(ownerContribution, {
            amountMinor: 1000
        })
    );

    await assertSucceeds(getDoc(ownerContribution));

    await assertSucceeds(
        updateDoc(ownerContribution, {
            amountMinor: 2000
        })
    );

    await assertSucceeds(
        getDocs(collection(aliceDb(), contributionsPath))
    );

    await assertSucceeds(deleteDoc(ownerContribution));

    await seedDocument(contributionPath, {
        amountMinor: 1000
    });

    const bobContribution = doc(bobDb(), contributionPath);
    const signedOutContribution = doc(signedOutDb(), contributionPath);

    await assertFails(getDoc(bobContribution));
    await assertFails(
        setDoc(
            doc(bobDb(), contributionsPath, "bob-new"),
            { amountMinor: 3000 }
        )
    );
    await assertFails(updateDoc(bobContribution, { amountMinor: 3000 }));
    await assertFails(deleteDoc(bobContribution));
    await assertFails(
        getDocs(collection(bobDb(), contributionsPath))
    );

    await assertFails(getDoc(signedOutContribution));
    await assertFails(
        setDoc(
            doc(signedOutDb(), contributionsPath, "signed-out-new"),
            { amountMinor: 3000 }
        )
    );
    await assertFails(updateDoc(signedOutContribution, { amountMinor: 3000 }));
    await assertFails(deleteDoc(signedOutContribution));
    await assertFails(
        getDocs(collection(signedOutDb(), contributionsPath))
    );
});

test("unknown collections and arbitrary descendants remain denied", async () => {
    const db = aliceDb();
    const deniedCollections = [
        "public",
        `users/${ALICE}/privateNotes`,
        `users/${ALICE}/expenses/expense-1/private`,
        `users/${ALICE}/savingsGoals/goal-1/contributions/contribution-1/private`
    ];

    for (const collectionPath of deniedCollections) {
        const existingDocument = doc(db, collectionPath, "existing");
        const newDocument = doc(db, collectionPath, "new");

        await assertFails(setDoc(newDocument, { value: 1 }));
        await seedDocument(`${collectionPath}/existing`);
        await assertFails(getDoc(existingDocument));
        await assertFails(updateDoc(existingDocument, { value: 2 }));
        await assertFails(deleteDoc(existingDocument));
        await assertFails(getDocs(collection(db, collectionPath)));
    }
});
