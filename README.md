# Reservation Manager
Demo: https://www.youtube.com/watch?v=_9kT4TsdjIE
@TODO / if we had more time
1) Don't store appts in memory, store in db or local file
2) Create a utility method to check for isWithin24Hrs that addAvailability, refreshPendingReservations, & refreshOpenReservations can all reuse.
2) getConfirmedReservations endpoint response should be consistent with the other endpoints, not just raw hashmap data. (although getConfirmedReservations not listed as a requirement)
3) create endpoint to view pending reservations.  Since we don't have this, we can test functionality by setting timeout to 5 seconds instead of 30 min, and watch reservations disappear and reappear in openreservations
4) Unit Tests for Reservation Service was rushed.  Each case should've been split to its own test method.
4) unit tests for reservation controller.  We should test the stream functions, but I prioritized testing the reservation service
5) create app setting for appointment length
6) Add functionality to remove available appts (workaround is for providers to book their own appts)
7) Add user roles and auth? but definitely not in 2 hrs
8) Create a POST endpoint to allow providers to add multiple days' worth of availability
9) Maybe figure out a different data structure for pending reservations. (db table with TTL would solve this issue, (kind of, but we'd still need to re-add the reservations to open))
   10) I originally wanted to use a Queue because this would allow us to poll and process expired pending reservations in the order they were added, but confirming appts would take O(n) time instead of O(1)
   11) but using a Queue isn't that much better because worst case is still O(n) if all reservations in the queue are expired
12) One potentially serious issue is that if we have existing confirmed reservations, and we change the reservation length in the util method, we will effectively change the times of all confirmed reservations because the timeblock maps to a different time.
13) We should have an endpoint to remove confirmed reservations for cancellations or past appointments
