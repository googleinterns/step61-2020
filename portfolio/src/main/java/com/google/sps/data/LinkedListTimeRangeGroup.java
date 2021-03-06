package com.google.sps.data;

import java.time.Instant;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Models an implementation of the TimeRangeGroup model using LinkedList. This is a slightly
 * modified version of the ArrayList implementation.
 */
public class LinkedListTimeRangeGroup extends AbstractListTimeRangeGroup implements TimeRangeGroup {

  /**
   * Adds all the input time ranges to the list of all time ranges. Also sorts the list of all
   * ranges in the constructor.
   */
  public LinkedListTimeRangeGroup(Iterable<TimeRange> timeRanges) {
    allTimeRanges = new LinkedList<TimeRange>();
    addAllTimeRanges(timeRanges);
  }

  /**
   * Adds a new time range to the list. If the time range to add overlaps with any existing time
   * range, the overlapping time ranges will be merged.
   */
  @Override
  public void addTimeRange(TimeRange timeRange) {
    // If the original allTimeRanges list is empty,
    // this is the first time we add anything to the list,
    // so simply add the new time range to the list and return.
    if (allTimeRanges.isEmpty()) {
      allTimeRanges.add(timeRange);
      return;
    }
    ListIterator<TimeRange> iterator = allTimeRanges.listIterator();

    // This variable represents the latest time range previously exmamined.
    // Initially, this variable points to the time range we want to add.
    // As the for loop iterates through all the current time ranges already existing
    // in allTimeRanges, this lastExaminedTimeRange variable is the time range with which
    // any existing time range merges with, if necessary.
    TimeRange lastExaminedTimeRange = timeRange;

    // We need to start off with a currentRange inside the loop
    TimeRange currentRange = iterator.next();

    // The reason I used a true is because moving back and forth in the
    // linked list can be confusing. Each case should be handled differently
    // so using iterator.hasNext() and currentRange=iterator.next() in each
    // iteration of this loop would make things harder to keep track of.
    while (true) {

      // If the current range is completely contained by the lastExaminedTimeRange,
      // we need to remove the current range from the list as lastExaminedTimeRange
      // will eventually replace it.
      if (lastExaminedTimeRange.contains(currentRange)
          && !lastExaminedTimeRange.equals(currentRange)) {
        iterator.remove();
        if (iterator.hasNext()) {
          currentRange = iterator.next();
        }
      }
      if (lastExaminedTimeRange.overlaps(currentRange)
          && !lastExaminedTimeRange.contains(currentRange)) {
        // The case for when two time ranges need to be merged.
        // Similar to the case above, it will eventually be replaced so we can
        // remove currentRange
        lastExaminedTimeRange = mergeTwoTimeRanges(currentRange, lastExaminedTimeRange);
        iterator.remove();
      } else if (currentRange.end().isAfter(lastExaminedTimeRange.end())) {
        // This is the case that lastExaminedTimeRange and the current time range do not overlap.
        // If the current range from the original list ends after the last examined time range,
        // add the time range pointed to by lastExaminedTimeRange to the new list before the current
        // range,and change the lastExaminedTimeRange to point to the current range.
        iterator.previous();
        iterator.add(lastExaminedTimeRange);
        iterator.next();
        lastExaminedTimeRange = currentRange;
      }

      // If current time range is the last element in the allTimeRanges list,
      // then there are two cases: if we just updated our pointer and are at
      // the same element then there is no need to add it. However, if
      // lastExaminedTimeRange does not match, this means lastExaminedTimeRange
      // actually belongs at the end of the new list.
      // Note: we do not use TimeRange's equals() method here but compare the objects directly.
      // If currentRange and lastExaminedTimeRange both point to the same time range object,
      // then lastExaminedTimeRange should not be added to the new list. However,
      // the result after merging currentRange with lastExaminedTimeRange could
      // potentially lead to a new time range object with the same
      // start and end time as currentRange's. If this is the case, we still want to add
      // lastExaminedTimeRange to the new list, because lastExaminedTimeRange stores
      // the result of merging.
      if (!iterator.hasNext()) {
        if (currentRange != lastExaminedTimeRange) {
          iterator.add(lastExaminedTimeRange);
        }
        // Once we add the last element we should end the
        // loop because it is the final range in the process.
        break;
      }
      // We move forward
      currentRange = iterator.next();
    }
  }

  /**
   * Checks if a time range exists in the collection. For example, if [3:00 - 4:00] is in the
   * collection, [3:00 - 3:30] is considered to exist as a time range in the collection. This method
   * uses linear search to find the time ranges whose start time is before the target range's start
   * and whose end time is after the target range's end. Then the method calls contains to see if
   * the target range is contained within this current range.
   */
  @Override
  public boolean hasTimeRange(TimeRange timeRangeToCheck) {
    for (TimeRange currentRange : allTimeRanges) {
      if (currentRange.contains(timeRangeToCheck)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Delete a time range from the list. Because the list of all time ranges are always kept to be
   * pairwise disjoint, the potentially two new time ranges resulted from a deletion will not
   * overlap with any other existing time ranges.
   *
   * <p>For example, if the list contains [3:00 - 4:00] and [5:00 - 6:00], deleting [3:15 - 3:30]
   * will result in [3 - 3:15] and [3:30 - 4] as new time ranges.
   *
   * <p>Another example for deleting overlapping time ranges: if [3 - 4] and [5 - 6] are in the
   * original list, deleting [3:30 - 5:30] will result in two new ranges: [3 - 3:30] and [5:30 - 6].
   */
  @Override
  public void deleteTimeRange(TimeRange timeRangeToDelete) {
    ListIterator<TimeRange> iterator = allTimeRanges.listIterator();

    while (iterator.hasNext()) {
      TimeRange currentRange = iterator.next();
      if (!currentRange.overlaps(timeRangeToDelete)) {
        continue;
      }
      Instant currentRangeStart = currentRange.start();
      Instant currentRangeEnd = currentRange.end();
      Instant toDeleteRangeStart = timeRangeToDelete.start();
      Instant toDeleteRangeEnd = timeRangeToDelete.end();

      // If currentRange overlaps then it is about to be modified so we
      // remove it and later add the fixed versions.
      iterator.remove();

      // Construct one or two new time ranges after the deletion.
      if (currentRangeStart.isBefore(toDeleteRangeStart)) {
        iterator.add(TimeRange.fromStartEnd(currentRangeStart, toDeleteRangeStart));
      }

      if (currentRangeEnd.isAfter(toDeleteRangeEnd)) {
        iterator.add(TimeRange.fromStartEnd(toDeleteRangeEnd, currentRangeEnd));
      }
    }
  }
}
