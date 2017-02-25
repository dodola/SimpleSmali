package dodola.flower.dex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction31t;
import org.jf.dexlib2.util.InstructionOffsetMap;
import org.jf.dexlib2.util.InstructionOffsetMap.InvalidInstructionOffset;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.ExceptionWithContext;
import org.jf.util.SparseIntArray;

import java.util.*;

public class MethodDefinition {
    public final Method method;
    public final MethodImplementation methodImpl;
    public final ImmutableList<Instruction> instructions;
    public final List<Instruction> effectiveInstructions;

    public final ImmutableList<MethodParameter> methodParameters;

    private final LabelCache labelCache = new LabelCache();

    private final SparseIntArray packedSwitchMap;
    private final SparseIntArray sparseSwitchMap;
    private final InstructionOffsetMap instructionOffsetMap;

    public MethodDefinition(Method method, MethodImplementation methodImpl) {
        this.method = method;
        this.methodImpl = methodImpl;

        try {

            instructions = ImmutableList.copyOf(methodImpl.getInstructions());
            methodParameters = ImmutableList.copyOf(method.getParameters());

            effectiveInstructions = Lists.newArrayList(instructions);

            packedSwitchMap = new SparseIntArray(0);
            sparseSwitchMap = new SparseIntArray(0);
            instructionOffsetMap = new InstructionOffsetMap(instructions);

            int endOffset = instructionOffsetMap.getInstructionCodeOffset(instructions.size() - 1) + instructions
                    .get(instructions.size() - 1).getCodeUnits();

            for (int i = 0; i < instructions.size(); i++) {
                Instruction instruction = instructions.get(i);

                Opcode opcode = instruction.getOpcode();
                if (opcode == Opcode.PACKED_SWITCH) {
                    boolean valid = true;
                    int codeOffset = instructionOffsetMap.getInstructionCodeOffset(i);
                    int targetOffset = codeOffset + ((OffsetInstruction) instruction).getCodeOffset();
                    try {
                        targetOffset = findPayloadOffset(targetOffset, Opcode.PACKED_SWITCH_PAYLOAD);
                    } catch (InvalidSwitchPayload ex) {
                        valid = false;
                    }
                    if (valid) {
                        if (packedSwitchMap.get(targetOffset, -1) != -1) {
                            Instruction payloadInstruction =
                                    findSwitchPayload(targetOffset, Opcode.PACKED_SWITCH_PAYLOAD);
                            targetOffset = endOffset;
                            effectiveInstructions.set(i,
                                    new ImmutableInstruction31t(opcode, ((Instruction31t) instruction).getRegisterA(),
                                            targetOffset - codeOffset));
                            effectiveInstructions.add(payloadInstruction);
                            endOffset += payloadInstruction.getCodeUnits();
                        }
                        packedSwitchMap.append(targetOffset, codeOffset);
                    }
                } else if (opcode == Opcode.SPARSE_SWITCH) {
                    boolean valid = true;
                    int codeOffset = instructionOffsetMap.getInstructionCodeOffset(i);
                    int targetOffset = codeOffset + ((OffsetInstruction) instruction).getCodeOffset();
                    try {
                        targetOffset = findPayloadOffset(targetOffset, Opcode.SPARSE_SWITCH_PAYLOAD);
                    } catch (InvalidSwitchPayload ex) {
                        valid = false;
                        // The offset to the payload instruction was invalid. Nothing to do, except that we won't
                        // add this instruction to the map.
                    }
                    if (valid) {
                        if (sparseSwitchMap.get(targetOffset, -1) != -1) {
                            Instruction payloadInstruction =
                                    findSwitchPayload(targetOffset, Opcode.SPARSE_SWITCH_PAYLOAD);
                            targetOffset = endOffset;
                            effectiveInstructions.set(i,
                                    new ImmutableInstruction31t(opcode, ((Instruction31t) instruction).getRegisterA(),
                                            targetOffset - codeOffset));
                            effectiveInstructions.add(payloadInstruction);
                            endOffset += payloadInstruction.getCodeUnits();
                        }
                        sparseSwitchMap.append(targetOffset, codeOffset);
                    }
                }
            }
        } catch (Exception ex) {
            String methodString;
            try {
                methodString = ReferenceUtil.getMethodDescriptor(method);
            } catch (Exception ex2) {
                throw ExceptionWithContext.withContext(ex, "Error while processing method");
            }
            throw ExceptionWithContext.withContext(ex, "Error while processing method %s", methodString);
        }
    }

    public Instruction findSwitchPayload(int targetOffset, Opcode type) {
        int targetIndex;
        try {
            targetIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(targetOffset);
        } catch (InvalidInstructionOffset ex) {
            throw new InvalidSwitchPayload(targetOffset);
        }

        //TODO: does dalvik let you pad with multiple nops?
        //TODO: does dalvik let a switch instruction point to a non-payload instruction?

        Instruction instruction = instructions.get(targetIndex);
        if (instruction.getOpcode() != type) {
            // maybe it's pointing to a NOP padding instruction. Look at the next instruction
            if (instruction.getOpcode() == Opcode.NOP) {
                targetIndex += 1;
                if (targetIndex < instructions.size()) {
                    instruction = instructions.get(targetIndex);
                    if (instruction.getOpcode() == type) {
                        return instruction;
                    }
                }
            }
            throw new InvalidSwitchPayload(targetOffset);
        } else {
            return instruction;
        }
    }

    public int findPayloadOffset(int targetOffset, Opcode type) {
        int targetIndex;
        try {
            targetIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(targetOffset);
        } catch (InvalidInstructionOffset ex) {
            throw new InvalidSwitchPayload(targetOffset);
        }

        //TODO: does dalvik let you pad with multiple nops?
        //TODO: does dalvik let a switch instruction point to a non-payload instruction?

        Instruction instruction = instructions.get(targetIndex);
        if (instruction.getOpcode() != type) {
            // maybe it's pointing to a NOP padding instruction. Look at the next instruction
            if (instruction.getOpcode() == Opcode.NOP) {
                targetIndex += 1;
                if (targetIndex < instructions.size()) {
                    instruction = instructions.get(targetIndex);
                    if (instruction.getOpcode() == type) {
                        return instructionOffsetMap.getInstructionCodeOffset(targetIndex);
                    }
                }
            }
            throw new InvalidSwitchPayload(targetOffset);
        } else {
            return targetOffset;
        }
    }

    public LabelCache getLabelCache() {
        return labelCache;
    }

    public int getPackedSwitchBaseAddress(int packedSwitchPayloadCodeOffset) {
        return packedSwitchMap.get(packedSwitchPayloadCodeOffset, -1);
    }

    public int getSparseSwitchBaseAddress(int sparseSwitchPayloadCodeOffset) {
        return sparseSwitchMap.get(sparseSwitchPayloadCodeOffset, -1);
    }

    private boolean needsAnalyzed() {
        for (Instruction instruction : methodImpl.getInstructions()) {
            if (instruction.getOpcode().odexOnly()) {
                return true;
            }
        }
        return false;
    }

    private void setLabelSequentialNumbers() {
        HashMap<String, Integer> nextLabelSequenceByType = new HashMap<String, Integer>();
        ArrayList<LabelMethodItem> sortedLabels = new ArrayList<LabelMethodItem>(labelCache.getLabels());

        //sort the labels by their location in the method
        Collections.sort(sortedLabels);

        for (LabelMethodItem labelMethodItem : sortedLabels) {
            Integer labelSequence = nextLabelSequenceByType.get(labelMethodItem.getLabelPrefix());
            if (labelSequence == null) {
                labelSequence = 0;
            }
            labelMethodItem.setLabelSequence(labelSequence);
            nextLabelSequenceByType.put(labelMethodItem.getLabelPrefix(), labelSequence + 1);
        }
    }

    public static class LabelCache {
        protected HashMap<LabelMethodItem, LabelMethodItem> labels = new HashMap<LabelMethodItem, LabelMethodItem>();

        public LabelCache() {
        }

        public LabelMethodItem internLabel(LabelMethodItem labelMethodItem) {
            LabelMethodItem internedLabelMethodItem = labels.get(labelMethodItem);
            if (internedLabelMethodItem != null) {
                return internedLabelMethodItem;
            }
            labels.put(labelMethodItem, labelMethodItem);
            return labelMethodItem;
        }

        public Collection<LabelMethodItem> getLabels() {
            return labels.values();
        }
    }

    public static class InvalidSwitchPayload extends ExceptionWithContext {
        private final int payloadOffset;

        public InvalidSwitchPayload(int payloadOffset) {
            super("No switch payload at offset: %d", payloadOffset);
            this.payloadOffset = payloadOffset;
        }

        public int getPayloadOffset() {
            return payloadOffset;
        }
    }
}
