# CA-68 offline and network-status manual test plan

## Preconditions

- Use a debug build on an Android emulator or device with a known API level. Record the device, API level, and test date.
- Sign in to a test account with an expense category available. Keep a second, refreshed session or Firebase console access for later server-side verification.
- Start with working internet access and a reachable Firestore backend. Use a test expense that can be identified without including private account data in evidence.
- This plan has not been executed. Android's validated network status is an indicator, not proof of Firestore synchronization.

## Online cache priming

1. Open the expense list for a chosen month while online. Confirm that one or more expenses load from Firestore and note an identifying test record.
2. Open the expense form and make sure its categories and other required values have loaded. Use the base currency for the offline entry to avoid depending on a new exchange rate.
3. Confirm that no offline banner is visible while the device has a validated internet connection.

## Airplane mode and offline entry

4. Enable airplane mode and disable Wi-Fi if it remains enabled. Wait for the global banner: “Offline — cached data is available. Changes will sync when connected.” Confirm it appears below the status bar and navigation remains usable.
5. Reopen the previously loaded expense month. Confirm its cached expenses remain visible. A month never loaded online is not expected to have cached records.
6. Enter a distinct test expense and save it. Confirm the form can complete without waiting indefinitely and the expense appears locally in the list.
7. Record this result as **locally queued, not server-confirmed**. Do not describe the expense as synced while the device is offline.

## Reconnection and synchronization

8. Disable airplane mode and restore a validated internet connection. Confirm the offline banner disappears and navigation remains usable.
9. Wait for Firestore to process pending writes. Check the distinct expense through Firebase or another refreshed session that reads from the server. Its presence there, rather than the banner disappearing, verifies synchronization.
10. If the expense is absent, retain the device state and collect logs or screenshots. Connectivity alone does not guarantee the write was accepted by Firestore.

## Restart with cached data

11. After the online expense list has been loaded, close the app, disable connectivity again, and restart it. Confirm the offline banner appears and the previously loaded expense data is available from the persistent cache. Record any sign-in or screen limitations encountered offline.

The existing profile store uses a server-only read. If cold offline startup cannot reach the expense screen, record that separately from whether Firestore retained the cached expense data. This feature does not change profile or navigation behavior.

## Failure evidence

For each failed step, record:

| Device / API | Date | Step | Expected | Actual | Screenshot or log reference |
| --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |
