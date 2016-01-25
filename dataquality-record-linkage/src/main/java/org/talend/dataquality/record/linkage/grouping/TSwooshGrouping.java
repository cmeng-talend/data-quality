// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataquality.record.linkage.grouping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.talend.dataquality.matchmerge.Attribute;
import org.talend.dataquality.matchmerge.MatchMergeAlgorithm;
import org.talend.dataquality.matchmerge.Record;
import org.talend.dataquality.matchmerge.mfb.MatchResult;
import org.talend.dataquality.matchmerge.mfb.RecordGenerator;
import org.talend.dataquality.matchmerge.mfb.RecordIterator;
import org.talend.dataquality.matchmerge.mfb.RecordIterator.ValueGenerator;
import org.talend.dataquality.record.linkage.grouping.swoosh.DQAttribute;
import org.talend.dataquality.record.linkage.grouping.swoosh.DQMFB;
import org.talend.dataquality.record.linkage.grouping.swoosh.DQMFBRecordMerger;
import org.talend.dataquality.record.linkage.grouping.swoosh.DQRecordIterator;
import org.talend.dataquality.record.linkage.grouping.swoosh.RichRecord;
import org.talend.dataquality.record.linkage.grouping.swoosh.SurvivorShipAlgorithmParams;
import org.talend.dataquality.record.linkage.grouping.swoosh.SurvivorShipAlgorithmParams.SurvivorshipFunction;
import org.talend.dataquality.record.linkage.record.CombinedRecordMatcher;
import org.talend.dataquality.record.linkage.record.IRecordMatcher;
import org.talend.dataquality.record.linkage.utils.SurvivorShipAlgorithmEnum;
import org.talend.utils.collections.BidiMultiMap;

/**
 * Record grouping class with t-swoosh algorithm.
 * 
 */
public class TSwooshGrouping<TYPE> {

    List<RecordGenerator> rcdsGenerators = new ArrayList<RecordGenerator>();

    int totalCount = 0;

    AbstractRecordGrouping<TYPE> recordGrouping;

    BidiMultiMap<String, String> oldGID2New = new BidiMultiMap<String, String>();

    // Added TDQ-9320: to use the algorithm handle the record one by one
    private DQMFB algorithm;

    /**
     * DOC zhao TSwooshGrouping constructor comment.
     */
    public TSwooshGrouping(AbstractRecordGrouping<TYPE> recordGrouping) {
        this.recordGrouping = recordGrouping;
    }

    /**
     * Getter for oldGID2New.
     * 
     * @return the oldGID2New
     */
    public Map<String, String> getOldGID2New() {
        return this.oldGID2New;
    }

    /**
     * Recording matching with t-swoosh algorithm.
     * 
     * @param inputRow
     * @param matchingRule
     */
    public void addToList(final TYPE[] inputRow, List<List<Map<java.lang.String, java.lang.String>>> multiMatchRules) {
        totalCount++;
        java.lang.String attributeName = null;
        Map<java.lang.String, ValueGenerator> rcdMap = new LinkedHashMap<String, RecordIterator.ValueGenerator>();
        for (List<Map<java.lang.String, java.lang.String>> matchRule : multiMatchRules) {
            for (final Map<java.lang.String, java.lang.String> recordMap : matchRule) {
                attributeName = recordMap.get(IRecordGrouping.ATTRIBUTE_NAME);
                if (attributeName == null) {
                    // Dummy matcher
                    continue;
                }
                rcdMap.put(attributeName, new ValueGenerator() {

                    @Override
                    public int getColumnIndex() {
                        return Integer.valueOf(recordMap.get(IRecordGrouping.COLUMN_IDX));
                    }

                    @Override
                    public java.lang.String newValue() {
                        TYPE value = inputRow[Integer.valueOf(recordMap.get(IRecordGrouping.COLUMN_IDX))];
                        return value == null ? null : value.toString();
                    }
                });
            }
        }
        RecordGenerator rcdGen = new RecordGenerator();
        rcdGen.setMatchKeyMap(rcdMap);
        List<DQAttribute<?>> rowList = new ArrayList<DQAttribute<?>>();
        int colIdx = 0;
        for (TYPE attribute : inputRow) {
            DQAttribute<TYPE> attri = new DQAttribute<TYPE>(StringUtils.EMPTY, colIdx, attribute);
            rowList.add(attri);
            colIdx++;
        }
        rcdGen.setOriginalRow(rowList);
        rcdsGenerators.add(rcdGen);
    }

    public void swooshMatch(IRecordMatcher combinedRecordMatcher, SurvivorShipAlgorithmParams survParams) {
        swooshMatch(combinedRecordMatcher, survParams, new GroupingCallBack());
    }

    /**
     * DOC yyin Comment method "swooshMatch".
     * 
     * @param combinedRecordMatcher
     * @param survParams
     */
    private void swooshMatch(IRecordMatcher combinedRecordMatcher, SurvivorShipAlgorithmParams survParams,
            GroupingCallBack callBack) {
        algorithm = (DQMFB) createTswooshAlgorithm(combinedRecordMatcher, survParams, callBack);

        Iterator<Record> iterator = new DQRecordIterator(totalCount, rcdsGenerators);
        while (iterator.hasNext()) {

            algorithm.matchOneRecord(iterator.next());
        }
    }

    /**
     * DOC yyin Comment method "createTswooshAlgorithm".
     * 
     * @param combinedRecordMatcher
     * @param survParams
     * @return
     */
    private MatchMergeAlgorithm createTswooshAlgorithm(IRecordMatcher combinedRecordMatcher,
            SurvivorShipAlgorithmParams survParams, MatchMergeAlgorithm.Callback callback) {
        SurvivorShipAlgorithmEnum[] surviorShipAlgos = new SurvivorShipAlgorithmEnum[survParams.getSurviorShipAlgos().length];
        String[] funcParams = new String[surviorShipAlgos.length];
        int idx = 0;
        for (SurvivorshipFunction func : survParams.getSurviorShipAlgos()) {
            surviorShipAlgos[idx] = func.getSurvivorShipAlgoEnum();
            funcParams[idx] = func.getParameter();
            idx++;
        }
        return new DQMFB(combinedRecordMatcher, new DQMFBRecordMerger("MFB", funcParams, //$NON-NLS-1$
                surviorShipAlgos, survParams), callback);
    }

    // init the algorithm before do matching.
    public void initialMFBForOneRecord(IRecordMatcher combinedRecordMatcher, SurvivorShipAlgorithmParams survParams) {
        algorithm = (DQMFB) createTswooshAlgorithm(combinedRecordMatcher, survParams, new GroupingCallBack());
    }

    // do match on one single record
    public void oneRecordMatch(RichRecord printRcd) {
        algorithm.matchOneRecord(printRcd);
    }

    // get and output all result after all records finished
    public void afterAllRecordFinished() {
        List<Record> result = algorithm.getResult();
        outputResult(result);
    }

    /**
     * Output the result Result after all finished.
     * 
     * @param result
     */
    private void outputResult(List<Record> result) {
        for (Record rcd : result) {
            RichRecord printRcd = (RichRecord) rcd;
            output(printRcd);
        }
        totalCount = 0;
        rcdsGenerators.clear();
    }

    class GroupingCallBack implements MatchMergeAlgorithm.Callback {

        @Override
        public void onBeginRecord(Record record) {
            // Nothing todo
        }

        @Override
        public void onMatch(Record record1, Record record2, MatchResult matchResult) {

            // record1 and record2 must be RichRecord from DQ grouping implementation.
            RichRecord richRecord1 = (RichRecord) record1;
            RichRecord richRecord2 = (RichRecord) record2;

            String grpId1 = richRecord1.getGroupId();
            String grpId2 = richRecord2.getGroupId();
            if (grpId1 == null && grpId2 == null) {
                // Both records are original records.
                String gid = UUID.randomUUID().toString(); // Generate a new GID.
                richRecord1.setGroupId(gid);
                richRecord2.setGroupId(gid);
                // group size is 0 for none-master record
                richRecord1.setGrpSize(0);
                richRecord2.setGrpSize(0);

                richRecord1.setMaster(false);
                richRecord2.setMaster(false);

                output(richRecord1);
                output(richRecord2);

            } else if (grpId1 != null && grpId2 != null) {
                // Both records are merged records.
                richRecord2.setGroupId(grpId1);
                // Put into the map: <gid2,gid1>
                oldGID2New.put(grpId2, grpId1);
                // Update map where value equals to gid2
                List<String> keysOfGID2 = oldGID2New.getKeys(grpId2);
                if (keysOfGID2 != null) {
                    for (String key : keysOfGID2) {
                        oldGID2New.put(key, grpId1);
                    }
                }

            } else if (grpId1 == null) {
                // richRecord1 is original record
                // GID is the gid of record 2.
                richRecord1.setGroupId(richRecord2.getGroupId());
                // group size is 0 for none-master record
                richRecord1.setGrpSize(0);
                richRecord1.setMaster(false);

                output(richRecord1);

            } else {
                // richRecord2 is original record.
                // GID
                richRecord2.setGroupId(richRecord1.getGroupId());
                // group size is 0 for none-master record
                richRecord2.setGrpSize(0);
                richRecord2.setMaster(false);

                output(richRecord2);
            }
        }

        @Override
        public void onNewMerge(Record record) {
            // record must be RichRecord from DQ grouping implementation.
            RichRecord richRecord = (RichRecord) record;
            richRecord.setMaster(true);
            richRecord.setScore(1.0);
            if (record.getGroupId() != null) {
                richRecord.setMerged(true);
                richRecord.setGrpSize(richRecord.getRelatedIds().size());
                if (richRecord.getGroupQuality() == 0) {
                    // group quality will be the confidence (score) .
                    richRecord.setGroupQuality(record.getConfidence());
                }
            }
        }

        @Override
        public void onRemoveMerge(Record record) {
            // record must be RichRecord from DQ grouping implementation.
            RichRecord richRecord = (RichRecord) record;
            if (richRecord.isMerged()) {
                richRecord.setOriginRow(null); // set null original row, won't be usefull anymore after another merge.
                richRecord.setGroupQuality(0);
            }
            richRecord.setMerged(false);
            richRecord.setMaster(false);
        }

        @Override
        public void onDifferent(Record record1, Record record2, MatchResult matchResult) {
            RichRecord currentRecord = (RichRecord) record2;
            currentRecord.setMaster(true);
            // The rest of group properties will be set in RichRecord$getOutputRow()
        }

        @Override
        public void onEndRecord(Record record) {
            // Nothing todo
        }

        @Override
        public boolean isInterrupted() {
            // Nothing todo
            return false;
        }

        @Override
        public void onBeginProcessing() {
            // Nothing todo
        }

        @Override
        public void onEndProcessing() {
            // Nothing todo
        }

    }

    private void output(RichRecord record) {
        recordGrouping.outputRow(record);
    }

    /**
     * Before match, move the record(not master) out, only use the master to match. (but need to remember the old GID)
     * 
     * After match: if 1(1,2) combined with 3(3,4), and 3 is the new master,-> 3(1,3), and now we should merge the 2,
     * and 4 which didnot attend the second tMatchgroup,--> 3(1,2,3,4), and the group size also need to be changed from
     * 2 to 4.
     * 
     * @param indexGID
     */
    public void swooshMatchWithMultipass(CombinedRecordMatcher combinedRecordMatcher,
            SurvivorShipAlgorithmParams survivorShipAlgorithmParams, int indexGID) {
        // key:GID, value: list of rows in this group which are not master.
        Map<String, List<List<DQAttribute<?>>>> groupRows = new HashMap<String, List<List<DQAttribute<?>>>>();
        List<RecordGenerator> notMasterRecords = new ArrayList<RecordGenerator>();
        for (RecordGenerator record : rcdsGenerators) {
            List<DQAttribute<?>> originalRow = record.getOriginalRow();
            if (!StringUtils.equalsIgnoreCase("true", originalRow.get(indexGID + 2).getValue())) {
                List<List<DQAttribute<?>>> list = groupRows.get(originalRow.get(indexGID).getValue());
                if (list == null) {
                    list = new ArrayList<List<DQAttribute<?>>>();
                    list.add(originalRow);
                    groupRows.put(originalRow.get(indexGID).getValue(), list);
                } else {
                    list.add(originalRow);
                }
                notMasterRecords.add(record);
            } else {
                DQAttribute<?> groupSize = originalRow.get(indexGID + 1);
                groupSize.setValue("0");
                DQAttribute<?> isMaster = originalRow.get(indexGID + 2);
                isMaster.setValue("false");
                // originalRow.set(indexGID + 1, groupSize);
            }
        }

        // remove the not masters before match
        rcdsGenerators.removeAll(notMasterRecords);
        totalCount = totalCount - notMasterRecords.size();

        // match the masters
        MultiPassGroupingCallBack multiPassGroupingCallBack = new MultiPassGroupingCallBack();
        multiPassGroupingCallBack.setGIDindex(indexGID);
        swooshMatch(combinedRecordMatcher, survivorShipAlgorithmParams, multiPassGroupingCallBack);

        // add the not masters again
        List<Record> result = algorithm.getResult();
        // List<Record> needAdd = new ArrayList<Record>();
        for (Record master : result) {
            String oldGID = oldGID2New.get(master.getGroupId());
            List<List<DQAttribute<?>>> list = groupRows.get(oldGID);
            if (list != null) {
                for (List<DQAttribute<?>> record : list) {
                    RichRecord createRecord = createRecord(record, master.getGroupId());
                    output(createRecord);
                }
            }
        }

    }

    private RichRecord createRecord(List<DQAttribute<?>> originalRow, String groupID) {
        List<Attribute> rowList = new ArrayList<Attribute>();
        for (DQAttribute<?> attr : originalRow) {
            rowList.add(attr);
        }

        RichRecord record = new RichRecord(rowList, originalRow.get(0).getValue(), 0, "MFB"); //$NON-NLS-1$

        record.setGroupId(groupID);
        record.setGrpSize(0);
        record.setMaster(false);
        record.setOriginRow(originalRow);
        record.setRecordSize(originalRow.size());
        return record;
    }

    class MultiPassGroupingCallBack extends GroupingCallBack {

        int indexGID = 0;

        public void setGIDindex(int index) {
            indexGID = index;
        }

        @Override
        public void onMatch(Record record1, Record record2, MatchResult matchResult) {
            // record1 and record2 must be RichRecord from DQ grouping implementation.
            RichRecord richRecord1 = (RichRecord) record1;
            RichRecord richRecord2 = (RichRecord) record2;

            String grpId1 = richRecord1.getOriginRow().get(indexGID).getValue();
            richRecord1.setGroupId(grpId1);
            String grpId2 = richRecord2.getOriginRow().get(indexGID).getValue();
            // Both records are merged records.
            richRecord2.setGroupId(grpId1);
            // Put into the map: <gid2,gid1>
            oldGID2New.put(grpId2, grpId1);
            // Update map where value equals to gid2
            List<String> keysOfGID2 = oldGID2New.getKeys(grpId2);
            if (keysOfGID2 != null) {
                for (String key : keysOfGID2) {
                    oldGID2New.put(key, grpId1);
                }
            }
            output(richRecord1);
            output(richRecord2);
        }

        @Override
        public void onNewMerge(Record record) {
            // record must be RichRecord from DQ grouping implementation.
            RichRecord richRecord = (RichRecord) record;
            richRecord.setMaster(true);
            richRecord.setScore(1.0);
            richRecord.setGroupId(richRecord.getOriginRow().get(indexGID).getValue());
            richRecord.setMerged(true);
            richRecord.setGrpSize(richRecord.getRelatedIds().size());
            if (richRecord.getGroupQuality() == 0) {
                // group quality will be the confidence (score) .
                richRecord.setGroupQuality(record.getConfidence());
            }
        }

        @Override
        public void onRemoveMerge(Record record) {
            // record must be RichRecord from DQ grouping implementation.
            RichRecord richRecord = (RichRecord) record;
            if (richRecord.isMerged()) {
                // removeOldValues(richRecord);
                richRecord.setGroupQuality(0);
            }
            richRecord.setMerged(false);
            richRecord.setMaster(false);
        }

    }
}
