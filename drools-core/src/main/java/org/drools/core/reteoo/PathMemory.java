package org.drools.core.reteoo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.drools.core.base.mvel.MVELSalienceExpression;
import org.drools.core.common.ActivationsFilter;
import org.drools.core.common.DefaultAgenda;
import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalAgendaGroup;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.Memory;
import org.drools.core.common.NetworkNode;
import org.drools.core.phreak.RuleAgendaItem;
import org.drools.core.phreak.TupleEntry;
import org.drools.core.util.AbstractBaseLinkedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathMemory extends AbstractBaseLinkedListNode<Memory>
        implements
        Memory {
    protected static transient Logger log = LoggerFactory.getLogger(PathMemory.class);
    private          long            linkedSegmentMask;
    private          long            allLinkedMaskTest;
    private          NetworkNode     networkNode;
    private volatile RuleAgendaItem  agendaItem;
    private          SegmentMemory[] segmentMemories;
    private          SegmentMemory   segmentMemory;
    private          Queue           queue;

    public PathMemory(NetworkNode networkNode) {
        this.networkNode = networkNode;
        this.queue = new ConcurrentLinkedQueue();
    }

    public Queue<TupleEntry> getQueue() {
        return queue;
    }

    public NetworkNode getNetworkNode() {
        return networkNode;
    }

    public RuleAgendaItem getRuleAgendaItem() {
        return agendaItem;
    }

    public void setlinkedSegmentMask(long mask) {
        linkedSegmentMask = mask;
    }

    public long getLinkedSegmentMask() {
        return linkedSegmentMask;
    }

    public long getAllLinkedMaskTest() {
        return allLinkedMaskTest;
    }

    public void setAllLinkedMaskTest(long allLinkedTestMask) {
        this.allLinkedMaskTest = allLinkedTestMask;
    }

    public void linkNodeWithoutRuleNotify(long mask) {
        linkedSegmentMask = linkedSegmentMask | mask;
    }

    public void linkSegment(long mask,
                            InternalWorkingMemory wm) {
        linkedSegmentMask = linkedSegmentMask | mask;
        if (log.isTraceEnabled()) {
            if (NodeTypeEnums.isTerminalNode(getNetworkNode())) {
                TerminalNode rtn = (TerminalNode) getNetworkNode();
                log.trace("  LinkSegment smask={} rmask={} name={}", mask, linkedSegmentMask, rtn.getRule().getName());
            } else {
                log.trace("  LinkSegment smask={} rmask={} name={}", mask, "RiaNode");
            }
        }
        if (isRuleLinked()) {
            doLinkRule(wm);
        }
    }

    public synchronized void doLinkRule(InternalWorkingMemory wm) {
        TerminalNode rtn = (TerminalNode) getNetworkNode();
        if (log.isTraceEnabled()) {
            log.trace("    LinkRule name={}", rtn.getRule().getName());
        }
        if (agendaItem == null) {
            int salience = ( rtn.getRule().getSalience() instanceof MVELSalienceExpression)
                           ? 0
                           : rtn.getRule().getSalience().getValue(null, rtn.getRule(), wm);
            agendaItem = ((InternalAgenda) wm.getAgenda()).createRuleAgendaItem(salience, this, rtn);
        }

        queueRuleAgendaItem(wm);
    }

    public synchronized void doUnlinkRule(InternalWorkingMemory wm) {
        TerminalNode rtn = (TerminalNode) getNetworkNode();
        if (log.isTraceEnabled()) {
            log.trace("    UnlinkRule name={}", rtn.getRule().getName());
        }
        if (agendaItem == null) {
            int salience = ( rtn.getRule().getSalience() instanceof MVELSalienceExpression)
                           ? 0
                           : rtn.getRule().getSalience().getValue(null, rtn.getRule(), wm);
            agendaItem = ((InternalAgenda) wm.getAgenda()).createRuleAgendaItem(salience, this, rtn);
        }

        queueRuleAgendaItem(wm);
    }

    public void queueRuleAgendaItem(InternalWorkingMemory wm) {
        synchronized ( agendaItem ) {
            agendaItem.getRuleExecutor().setDirty(true);
            
            ActivationsFilter activationFilter = ((DefaultAgenda) wm.getAgenda()).getActivationsFilter();
            if ( activationFilter != null && !activationFilter.accept( agendaItem,
                                                                       wm,
                                                                       agendaItem.getTerminalNode() ) ) {
                return;
            }
            
            if (!agendaItem.isQueued() && !agendaItem.isBlocked()) {
                if ( log.isTraceEnabled() ) {
                    log.trace("Queue RuleAgendaItem {}", agendaItem);
                }
                InternalAgendaGroup ag = agendaItem.getAgendaGroup();
                ag.add( agendaItem );
            }
        }
        if ( agendaItem.getRule().isEager() ) {
            // will return if already added
            ((InternalAgenda)wm.getAgenda()).addEagerRuleAgendaItem( agendaItem );
        }
    }


    public void unlinkedSegment(long mask,
                                InternalWorkingMemory wm) {
        boolean linkedRule =  isRuleLinked();
        linkedSegmentMask = linkedSegmentMask ^ mask;
        if (log.isTraceEnabled()) {
            log.trace("  UnlinkSegment smask={} rmask={} name={}", mask, linkedSegmentMask, this);
        }
        if (linkedRule && !isRuleLinked()) {
            doUnlinkRule(wm);
        }
    }

    public boolean isRuleLinked() {
        return (linkedSegmentMask & allLinkedMaskTest) == allLinkedMaskTest;
    }

    public short getNodeType() {
        return NodeTypeEnums.RuleTerminalNode;
    }

    public SegmentMemory[] getSegmentMemories() {
        return segmentMemories;
    }

    public void setSegmentMemories(SegmentMemory[] segmentMemories) {
        this.segmentMemories = segmentMemories;
    }

    public SegmentMemory getSegmentMemory() {
        return this.segmentMemory;
    }

    public void setSegmentMemory(SegmentMemory sm) {
        this.segmentMemory = sm;
    }

    public String toString() {
        return "[RuleMem " + ((TerminalNode) getNetworkNode()).getRule().getName() + "]";
    }

}
