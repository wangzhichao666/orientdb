/*
 * Copyright 2018 OrientDB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.index;

import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author marko
 */
public class OLuceneTracker {

  private static OLuceneTracker instance = null;
  private final Map<Long, PerIndexWriterTRracker> mappedTrackers = new HashMap<>();
  private final Long[] locks = new Long[128];  
  private final Map<OLogSequenceNumber, List<Long>> involvedWriterIdsInLSN = new HashMap<>();
  
  public static OLuceneTracker instance() {
    if (instance == null) {
      synchronized (OLuceneTracker.class) {
        if (instance == null) {
          instance = new OLuceneTracker();
        }
      }
    }

    return instance;
  }

  private Long getLockObject(Long writerIndex){
    return writerIndex % locks.length;
  }
  
  
  public synchronized void mapWriterIdsToSpecificLSN(OLogSequenceNumber lsn, List<Long> writerIds){
    involvedWriterIdsInLSN.put(lsn, writerIds);
  }
  
  /**
   * inclusive
   */
  public synchronized Collection<OLogSequenceNumber> getPreviousLSNsTill(OLogSequenceNumber referent){
    List<OLogSequenceNumber> ret = new ArrayList<>();
    for (OLogSequenceNumber lsn : involvedWriterIdsInLSN.keySet()){
      if (lsn.compareTo(referent) <= 0){
        ret.add(lsn);
      }
    }
    return ret;
  }
  
  public synchronized Collection<Long> getMappedIndexWritersIds(Collection<OLogSequenceNumber> referentLSNs){
    Set<Long> ret = new HashSet<>();
    for (OLogSequenceNumber lsn : referentLSNs){
      Collection<Long> writerIds = involvedWriterIdsInLSN.get(lsn);
      if (writerIds != null){
        ret.addAll(writerIds);
      }
    }
    return ret;
  }
  
  public void track(ORecordId rec, long sequenceNumber, Long writerIndex){
    synchronized(getLockObject(writerIndex)){
      System.out.println("----------------------------SEQUENCE NUMBER: " + sequenceNumber + ", WRITER INDEX: " + writerIndex);
      PerIndexWriterTRracker tracker = mappedTrackers.get(writerIndex);
      if (tracker == null){
        tracker = new PerIndexWriterTRracker();
        mappedTrackers.put(writerIndex, tracker);
      }
      tracker.track(rec, sequenceNumber);
    }
  }
  
  public long getLargestSequenceNumber(List<ORecordId> observedIds, Long writerIndex){
    long retVal = -1;
    if (writerIndex == null){
      return retVal;
    }    
    synchronized(getLockObject(writerIndex)){
      PerIndexWriterTRracker tracker = mappedTrackers.get(writerIndex);
      if (tracker != null){
        long val = tracker.getLargestsequenceNumber(observedIds);
        if (val > retVal){
          System.out.println("GET LARGEST SEQUENCE NUMBER: " + val + " WriterID: " + writerIndex);
          retVal = val;
        }
      }      
    }
    return retVal;
  }
  
  public void mapLSNToHighestSequenceNumber(OLogSequenceNumber lsn, Long sequenceNumber, Long writerIndex){
    if (writerIndex == null){
      return;
    }    
    synchronized(getLockObject(writerIndex)){
      PerIndexWriterTRracker tracker = mappedTrackers.get(writerIndex);
      if (tracker != null){
        System.out.println("MAPPED LARGEST SEQUENCE NUMBER: " + sequenceNumber + " TO LSN: " + lsn + " WriterID: " + writerIndex);
        tracker.mapLSNToHighestSequenceNumber(lsn, sequenceNumber);
      }
    }    
  }
  
  public void clearMappedRidsToHighestSequenceNumbers(List<Long> writerIds, List<Long> highestSequenceNumbers){
    if (writerIds == null){
      return;
    }
    for (int i = 0; i < writerIds.size(); i++){
      Long writerId = writerIds.get(i);
      Long tillSequenceNumber = highestSequenceNumbers.get(i);
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        if (tracker != null){
          tracker.clearMappedRidsToHighestSequenceNumbers();
        }
      }
    }
  }
  
  public synchronized Long getHighestSequnceNumberCanBeFlushed(Long indexWriterId){    
    if (indexWriterId == null){
      return null;
    }
    
    synchronized(getLockObject(indexWriterId)){
      PerIndexWriterTRracker tracker = mappedTrackers.get(indexWriterId);
      if (tracker != null){
        return tracker.getHighestSequnceNumberCanBeFlushed();
      }
    }
    
    return null;
  }
  
  public void setHighestSequnceNumberCanBeFlushed(Long value, Long indexWriterId) {
    if (indexWriterId == null){
      return;
    }
    
    synchronized(getLockObject(indexWriterId)){
      PerIndexWriterTRracker tracker = mappedTrackers.get(indexWriterId);
      if (tracker != null){
        tracker.setHighestSequnceNumberCanBeFlushed(value);
      }
    }
    
  }
  
  public void setHighestFlushedSequenceNumber(Long value, Long writerId) {
    synchronized(getLockObject(writerId)){
      PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
      if (tracker != null){
        tracker.setHighestFlushedSequenceNumber(value);
      }
    }
  }
  
//  public Long getNearestSmallerOrEqualSequenceNumber(Long referentVal){    
//    Long retVal = null;
//    for (Long writerId : mappedTrackers.keySet()){
//      synchronized(getLockObject(writerId)){
//        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
//        Long tmpVal = tracker.getNearestSmallerOrEqualSequenceNumber(referentVal);
//        if ((tmpVal != null) && (retVal == null || tmpVal > retVal)){
//          retVal = tmpVal;
//        }
//      }
//    }
//    
//    return retVal;
//  }
  
  public OLogSequenceNumber getNearestSmallerOrEqualLSN(OLogSequenceNumber toLsn){
    OLogSequenceNumber retVal = null;
    for (Long writerId : mappedTrackers.keySet()){
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        OLogSequenceNumber tmpVal = tracker.getNearestSmallerOrEqualLSN(toLsn);
        if ((tmpVal != null) && (retVal == null || tmpVal.compareTo(retVal) > 0)){
          retVal = tmpVal;
        }
      }
    }
    
    return retVal;  
  }
  
  public Long getHighestMappedSequenceNumber(Collection<OLogSequenceNumber> observedLSNs, Long indexWriterId){
    if (indexWriterId == null){
      return null;
    }   
    synchronized(getLockObject(indexWriterId)){
      PerIndexWriterTRracker tracker = mappedTrackers.get(indexWriterId);
      Long tmpVal = tracker.getHighestMappedSequenceNumber(observedLSNs);
      if (tmpVal != null){
        return tmpVal;
      }
    }    
    
    return null;
  }
    
  public Long getHighestFlushedSequenceNumber(){
    Long retVal = null;
    for (Long writerId : mappedTrackers.keySet()){
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        Long tmpVal = tracker.getHighestFlushedSequenceNumber();
        if ((tmpVal != null) && (retVal == null || tmpVal > retVal)){
          retVal = tmpVal;
        }
      }
    }
    return retVal;
  }
  
  public OLogSequenceNumber getMappedLSN(Long sequenceNumber) {
    OLogSequenceNumber retVal = null;
    for (Long writerId : mappedTrackers.keySet()){
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        OLogSequenceNumber tmpVal = tracker.getMappedLSN(sequenceNumber);
        if (tmpVal != null){
          retVal = tmpVal;
          break;
        }
      }
    }
    return retVal;
  }
  
  public void resetHasUnflushedSequences(){    
    for (Long writerId : mappedTrackers.keySet()){
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        tracker.resetHasUnflushedSequences();
      }
    }    
  }
  
  public synchronized boolean hasUnflushedSequences(Collection<Long> writersIds) {
    for (Long writerId : writersIds){
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        if (tracker != null && tracker.hasUnflushedSequences()){
          return true;
        }
      }
    }
    return false;
  }
  
  public OLogSequenceNumber getMinimalFlushedLSNForAllWriters(Collection<Long> writerIds, OLogSequenceNumber referentLSN){
    OLogSequenceNumber ret = null;
    //in fact here we want to use minimum of LSNs mappped to each writers' highest flushed sequence number
    for (Long writerId : writerIds){      
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        Long highestFlushed = tracker.getHighestFlushedSequenceNumber();
        //if some writer still doesn't have flushed return null is minimal
        if (highestFlushed == null){
          return null;
        }
        Long mappedEquivalent = tracker.getNearestSmallerOrEqualSequenceNumber(highestFlushed);
        OLogSequenceNumber lsn = tracker.getMappedLSN(mappedEquivalent);        
        //this lsn must be smaller or equals than referent LSN
        if (lsn.compareTo(referentLSN) > 0){
          throw new ODatabaseException("Lucene index is desynchronized with database");
        }
        if (ret == null || lsn.compareTo(ret) < 0){
          ret = lsn;
        }
      }
    }
    
    return ret;
  }
  
  public void cleanUpTillLSN(Collection<Long> writerIds, OLogSequenceNumber toLSN){
    for (Long writerId : writerIds){      
      synchronized(getLockObject(writerId)){
        PerIndexWriterTRracker tracker = mappedTrackers.get(writerId);
        tracker.cleanUpTillLSN(toLSN);
      }
    }
  }
  
  private class PerIndexWriterTRracker {

    private final Map<ORecordId, Long> mappedHighestsequnceNumbers = new HashMap<>();
    private final Map<OLogSequenceNumber, Long> highestSequenceNumberForLSN = new HashMap<>();
    private final Map<Long, OLogSequenceNumber> LSNForHighestSequenceNumber = new HashMap<>();
    
    private Long highestFlushedSequenceNumber = null;
    private boolean hasUnflushedSequences = false;
    private AtomicLong highestSequenceNumberCanBeFlushed = null;

    public void track(ORecordId rec, long sequenceNumber) {
      hasUnflushedSequences = true;
      if (rec == null) {
        return;
      }            
      synchronized (mappedHighestsequnceNumbers) {
        Long val = mappedHighestsequnceNumbers.get(rec);
        if (val == null || val < sequenceNumber) {
          mappedHighestsequnceNumbers.put(rec, sequenceNumber);
        }
      }
    }

    public long getLargestsequenceNumber(List<ORecordId> observedIds) {
      long retVal = -1l;
      synchronized (mappedHighestsequnceNumbers) {
        for (ORecordId rec : observedIds) {
          Long val = mappedHighestsequnceNumbers.get(rec);
          if (val != null && val > retVal) {
            retVal = val;
          }
        }
      }
      return retVal;
    }

    public void clearMappedRidsToHighestSequenceNumbers(Long tillSequenceNumber) {
      synchronized(mappedHighestsequnceNumbers){
        List<ORecordId> toBeRemovedCollection = new ArrayList<>();
        for (Map.Entry<ORecordId, Long> entry : mappedHighestsequnceNumbers.entrySet()){
          if (entry.getValue() <= tillSequenceNumber){
            toBeRemovedCollection.add(entry.getKey());
          }
        }
        for (ORecordId toRemove : toBeRemovedCollection){
          mappedHighestsequnceNumbers.remove(toRemove);
        }
      }
    }

    public void mapLSNToHighestSequenceNumber(OLogSequenceNumber lsn, Long sequenceNumber) {
      if (lsn == null || sequenceNumber == null) {
        return;
      }
      synchronized (highestSequenceNumberForLSN) {
        highestSequenceNumberForLSN.put(lsn, sequenceNumber);
        LSNForHighestSequenceNumber.put(sequenceNumber, lsn);
      }
    }

    public Long getNearestSmallerOrEqualSequenceNumber(Long referentVal) {
      if (referentVal == null){
        return null;
      }
      synchronized (this) {
        Long[] tmpListForSort = LSNForHighestSequenceNumber.keySet().toArray(new Long[0]);
        Arrays.sort(tmpListForSort);
        //find last smaller then specified
        for (int i = tmpListForSort.length - 1; i >= 0; i--) {
          if (tmpListForSort[i] == null) {
            System.out.println("Index is: " + i);
          }
          if (tmpListForSort[i] <= referentVal) {
            return tmpListForSort[i];
          }
        }
      }

      return null;
    }

    public OLogSequenceNumber getNearestSmallerOrEqualLSN(OLogSequenceNumber toLsn) {
      synchronized (highestSequenceNumberForLSN) {
        OLogSequenceNumber[] tmpListForSort = highestSequenceNumberForLSN.keySet().toArray(new OLogSequenceNumber[0]);
        Arrays.sort(tmpListForSort);
        //find last smaller then specified
        for (int i = tmpListForSort.length - 1; i >= 0; i--) {
          if (tmpListForSort[i].compareTo(toLsn) <= 0) {
            return tmpListForSort[i];
          }
        }
      }

      return null;
    }

    public Long getHighestMappedSequenceNumber(Collection<OLogSequenceNumber> observedLSNs) {
      Long ret = null;
      synchronized(highestSequenceNumberForLSN){
        for (OLogSequenceNumber lsn : observedLSNs){
          Long val = highestSequenceNumberForLSN.get(lsn);
          if ((val != null) && (ret == null || val > ret)){
            ret = val;
          }
        }
      }
      
      return ret;
    }

    public synchronized OLogSequenceNumber getMappedLSN(Long sequenceNumber) {
      return LSNForHighestSequenceNumber.get(sequenceNumber);
    }    

    public synchronized void setHighestFlushedSequenceNumber(Long value) {
      highestFlushedSequenceNumber = value;
    }    

    public synchronized Long getHighestFlushedSequenceNumber() {
      return highestFlushedSequenceNumber;
    }

    public synchronized boolean hasUnflushedSequences() {
      return hasUnflushedSequences;
    }

    public synchronized void setHasUnflushedSequences(boolean hasUnflushedSequences) {
      this.hasUnflushedSequences = hasUnflushedSequences;
    }

    public void resetHasUnflushedSequences() {
      synchronized(mappedHighestsequnceNumbers){
        if (mappedHighestsequnceNumbers.isEmpty()) {
          hasUnflushedSequences = false;
        }
      }
    }

    private void setHighestSequnceNumberCanBeFlushed(Long value) {
      if (value == null){
        return;
      }
      if (highestSequenceNumberCanBeFlushed == null){
        synchronized (this){
          if (highestSequenceNumberCanBeFlushed == null){
            highestSequenceNumberCanBeFlushed = new AtomicLong();
          }
        }
      }
      highestSequenceNumberCanBeFlushed.set(value);
    }

    private Long getHighestSequnceNumberCanBeFlushed() {
      if (highestSequenceNumberCanBeFlushed == null){
        return null;
      }
      
      return highestSequenceNumberCanBeFlushed.get();
    }

    private void cleanUpTillLSN(OLogSequenceNumber toLSN) {
//      Long highestFlushed = getHighestFlushedSequenceNumber();
//      Long mappedEquivalent = getNearestSmallerOrEqualSequenceNumber(highestFlushed);
//      OLogSequenceNumber referentLSN = getMappedLSN(mappedEquivalent);
      Set<OLogSequenceNumber> toBeRemoved = new HashSet<>();
      for (OLogSequenceNumber lsn : highestSequenceNumberForLSN.keySet()){
        if (lsn.compareTo(toLSN) <= 0){
          toBeRemoved.add(lsn);
        }
      }
      for (OLogSequenceNumber toRemove : toBeRemoved){
        highestSequenceNumberForLSN.remove(toRemove);
      }
      
      //remove from mirror map
      List<Long> toBeRemovedMirror = new ArrayList<>();
      for (Long sequenceNumber : LSNForHighestSequenceNumber.keySet()){
        if (toBeRemoved.contains(LSNForHighestSequenceNumber.get(sequenceNumber))){
          toBeRemovedMirror.add(sequenceNumber);
        }
      }
      for (Long toRemove : toBeRemovedMirror){
        LSNForHighestSequenceNumber.remove(toRemove);
      }
    }
  }

}
