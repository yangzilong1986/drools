/*
 * Copyright 2005 JBoss Inc
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

package org.drools.core.reteoo;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.LeftTupleIterator;
import org.drools.core.common.PropagationContextImpl;
import org.drools.core.common.UpdateContext;
import org.drools.core.reteoo.ReteooWorkingMemory.QueryRiaFixerNodeFixer;
import org.drools.core.reteoo.builder.BuildContext;
import org.drools.core.spi.PropagationContext;
import org.drools.core.util.Iterator;

/**
 * Node which filters <code>ReteTuple</code>s.
 *
 * <p>
 * Using a semantic <code>Test</code>, this node may allow or disallow
 * <code>Tuples</code> to proceed further through the Rete-OO network.
 * </p>
 *
 * @see QueryRiaFixerNode
 * @see Eval
 * @see LeftTuple
 */
public class QueryRiaFixerNode extends LeftTupleSource
    implements
    LeftTupleSinkNode {
    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------

    private static final long serialVersionUID = 510l;

    protected boolean         tupleMemoryEnabled;

    private BetaNode          betaNode;
    
    private LeftTupleSinkNode previousTupleSinkNode;
    
    private LeftTupleSinkNode nextTupleSinkNode;    
    
    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------
    public QueryRiaFixerNode() {

    }

    /**
     * Construct.
     *
     * @param rule
     *            The rule
     * @param tupleSource
     *            The source of incoming <code>Tuples</code>.
     * @param eval
     */
    public QueryRiaFixerNode(final int id,
                             final LeftTupleSource tupleSource,
                             final BuildContext context) {
        super( id,
               context.getPartitionId(),
               context.getRuleBase().getConfiguration().isMultithreadEvaluation() );
        setLeftTupleSource(tupleSource);
        this.tupleMemoryEnabled = context.isTupleMemoryEnabled();
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        betaNode = (BetaNode) in.readObject();
        tupleMemoryEnabled = in.readBoolean();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        out.writeObject(   betaNode  );
        out.writeBoolean( tupleMemoryEnabled );
    }

    public BetaNode getBetaNode() {
        return betaNode;
    }

    @Override
    public void addTupleSink(LeftTupleSink tupleSink, BuildContext context) {
        this.betaNode = (BetaNode) tupleSink;
    }

    public void attach( BuildContext context ) {
        this.leftInput.addTupleSink( this, context );
        if (context == null || context.getRuleBase().getConfiguration().isPhreakEnabled() ) {
            return;
        }

        for ( InternalWorkingMemory workingMemory : context.getWorkingMemories() ) {
            final PropagationContext propagationContext = new PropagationContextImpl( workingMemory.getNextPropagationIdCounter(),
                                                                                      PropagationContext.RULE_ADDITION,
                                                                                      null,
                                                                                      null,
                                                                                      null );
            this.leftInput.updateSink( this,
                                         propagationContext,
                                         workingMemory );
        }
    }

    public void networkUpdated(UpdateContext updateContext) {
        this.leftInput.networkUpdated(updateContext);
    }

    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    // org.kie.reteoo.impl.TupleSink
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Assert a new <code>Tuple</code>.
     *
     * @param leftTuple
     *            The <code>Tuple</code> being asserted.
     * @param workingMemory
     *            The working memory seesion.
     * @throws AssertionException
     *             If an error occurs while asserting.
     */
    public void assertLeftTuple(final LeftTuple leftTuple,
                                final PropagationContext context,
                                final InternalWorkingMemory workingMemory) {
        context.getQueue2().addLast( new QueryRiaFixerNodeFixer(context, leftTuple, false, betaNode)  );
    }

    public void retractLeftTuple(final LeftTuple leftTuple,
                                 final PropagationContext context,
                                 final InternalWorkingMemory workingMemory) {
        context.getQueue2().addLast( new QueryRiaFixerNodeFixer(context, leftTuple, true, betaNode)  );
    }
    public void modifyLeftTuple(LeftTuple leftTuple,
                                PropagationContext context,
                                InternalWorkingMemory workingMemory) {        
        context.getQueue2().addLast( new QueryRiaFixerNodeFixer(context, leftTuple, false, betaNode)  );
    }

    /**
     * Produce a debug string.
     *
     * @return The debug string.
     */
    public String toString() {
        return "[RiaQueryFixerNode: ]";
    }

    public int hashCode() {
        return this.leftInput.hashCode();
    }

    public boolean equals(final Object object) {
        // we never node share, so only return true if we are instance equal
        if ( this == object ) {
            return true;
        }
        return false; 
    }

    public void updateSink(final LeftTupleSink sink,
                           final PropagationContext context,
                           final InternalWorkingMemory workingMemory) {
        Iterator<LeftTuple> it = LeftTupleIterator.iterator( workingMemory, this );
        
        for ( LeftTuple leftTuple =  it.next(); leftTuple != null; leftTuple = it.next() ) {
            LeftTuple childLeftTuple = leftTuple.getFirstChild();
            while ( childLeftTuple != null ) {
                RightTuple rightParent = childLeftTuple.getRightParent();            
                sink.assertLeftTuple( sink.createLeftTuple( leftTuple, rightParent, childLeftTuple, null, sink, true ),
                                      context,
                                      workingMemory );  
                
                while ( childLeftTuple != null && childLeftTuple.getRightParent() == rightParent ) {
                    // skip to the next child that has a different right parent
                    childLeftTuple = childLeftTuple.getLeftParentNext();
                }
            }
        }
    }

    protected void doRemove(final RuleRemovalContext context,
                            final ReteooBuilder builder,
                            final InternalWorkingMemory[] workingMemories) {
        if (!isInUse()) {
            getLeftTupleSource().removeTupleSink(this);
        }
    }

    protected void doCollectAncestors(NodeSet nodeSet) {
        getLeftTupleSource().collectAncestors(nodeSet);
    }

    public boolean isLeftTupleMemoryEnabled() {
        return tupleMemoryEnabled;
    }

    public void setLeftTupleMemoryEnabled(boolean tupleMemoryEnabled) {
        this.tupleMemoryEnabled = tupleMemoryEnabled;
    }

    /**
     * Returns the next node
     * @return
     *      The next TupleSinkNode
     */
    public LeftTupleSinkNode getNextLeftTupleSinkNode() {
        return this.nextTupleSinkNode;
    }

    /**
     * Sets the next node
     * @param next
     *      The next TupleSinkNode
     */
    public void setNextLeftTupleSinkNode(final LeftTupleSinkNode next) {
        this.nextTupleSinkNode = next;
    }

    /**
     * Returns the previous node
     * @return
     *      The previous TupleSinkNode
     */
    public LeftTupleSinkNode getPreviousLeftTupleSinkNode() {
        return this.previousTupleSinkNode;
    }

    /**
     * Sets the previous node
     * @param previous
     *      The previous TupleSinkNode
     */
    public void setPreviousLeftTupleSinkNode(final LeftTupleSinkNode previous) {
        this.previousTupleSinkNode = previous;
    }

    public short getType() {
        return NodeTypeEnums.QueryRiaFixerNode;
    }
    
    public LeftTuple createLeftTuple(InternalFactHandle factHandle,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return this.betaNode.createLeftTuple( factHandle, this.betaNode, leftTupleMemoryEnabled  );
    }

    public LeftTuple createLeftTuple(final InternalFactHandle factHandle,
                                     final LeftTuple leftTuple,
                                     final LeftTupleSink sink) {
        return this.betaNode.createLeftTuple(factHandle,leftTuple, sink );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     LeftTupleSink sink,
                                     PropagationContext pctx, boolean leftTupleMemoryEnabled) {
        return this.betaNode.createLeftTuple( leftTuple, this.betaNode, pctx, leftTupleMemoryEnabled);
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTupleSink sink) {
        return this.betaNode.createLeftTuple( leftTuple, rightTuple, this.betaNode );
    }   
    
    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTuple currentLeftChild,
                                     LeftTuple currentRightChild,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return this.betaNode.createLeftTuple( leftTuple, rightTuple, currentLeftChild, currentRightChild,  this.betaNode, leftTupleMemoryEnabled  );        
    }      


    @Override
    public boolean isInUse() {
        return this.betaNode != null;
    }

    public LeftTupleSource getLeftTupleSource() {
        return this.leftInput;
    }

    protected ObjectTypeNode getObjectTypeNode() {
        return leftInput.getObjectTypeNode();
    }

    @Override
    public LeftTuple createPeer(LeftTuple original) {
        return null;
    }

    @Override
    protected void removeTupleSink(LeftTupleSink tupleSink) {
        if (tupleSink != betaNode) {
            throw new IllegalArgumentException( tupleSink + " is not a sink for this node" );
        }
        betaNode = null;
    }
}
