package goodgenerator.blocks.tileEntity;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static goodgenerator.main.GGConfigLoader.*;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import goodgenerator.blocks.tileEntity.base.MTETooltipMultiBlockBaseEM;
import goodgenerator.items.GGMaterial;
import goodgenerator.loader.Loaders;
import goodgenerator.util.CrackRecipeAdder;
import goodgenerator.util.DescTextLocalization;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.MaterialsUEVplus;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEHatchDynamo;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchOutput;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gtPlusPlus.xmod.thermalfoundation.fluid.TFFluids;
import tectech.thing.metaTileEntity.hatch.MTEHatchDynamoMulti;

public class MTEMultiNqGenerator extends MTETooltipMultiBlockBaseEM implements IConstructable, ISurvivalConstructable {

    protected IStructureDefinition<MTEMultiNqGenerator> multiDefinition = null;
    protected long leftEnergy = 0;
    protected long trueOutput = 0;
    protected int trueEff = 0;
    protected FluidStack lockedFluid = null;
    protected int times = 1;
    protected int basicOutput;

    private static final List<Pair<FluidStack, Integer>> excitedLiquid;

    private static final List<Pair<FluidStack, Integer>> coolant;

    static {
        excitedLiquid = Arrays.asList(
            Pair.of(MaterialsUEVplus.Space.getMolten(20L), ExcitedLiquidCoe[0]),
            Pair.of(GGMaterial.atomicSeparationCatalyst.getMolten(20), ExcitedLiquidCoe[1]),
            Pair.of(Materials.Naquadah.getMolten(20L), ExcitedLiquidCoe[2]),
            Pair.of(Materials.Uranium235.getMolten(180L), ExcitedLiquidCoe[3]),
            Pair.of(Materials.Caesium.getMolten(180L), ExcitedLiquidCoe[4]));
        coolant = Arrays.asList(
            Pair.of(MaterialsUEVplus.Time.getMolten(20L), CoolantEfficiency[0]),
            Pair.of(new FluidStack(TFFluids.fluidCryotheum, 1_000), CoolantEfficiency[1]),
            Pair.of(Materials.SuperCoolant.getFluid(1_000), CoolantEfficiency[2]),
            Pair.of(GTModHandler.getIC2Coolant(1_000), CoolantEfficiency[3]));
    }

    @Override
    public void construct(ItemStack itemStack, boolean hintsOnly) {
        structureBuild_EM(mName, 3, 7, 0, itemStack, hintsOnly);
    }

    @Override
    public String[] getStructureDescription(ItemStack itemStack) {
        return DescTextLocalization.addText("MultiNqGenerator.hint", 8);
    }

    public final boolean addToGeneratorList(IGregTechTileEntity aTileEntity, int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        } else {
            IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
            if (aMetaTileEntity == null) {
                return false;
            } else {
                if (aMetaTileEntity instanceof MTEHatch) {
                    ((MTEHatch) aMetaTileEntity).updateTexture(aBaseCasingIndex);
                }
                if (aMetaTileEntity instanceof MTEHatchInput) {
                    return this.mInputHatches.add((MTEHatchInput) aMetaTileEntity);
                } else if (aMetaTileEntity instanceof MTEHatchOutput) {
                    return this.mOutputHatches.add((MTEHatchOutput) aMetaTileEntity);
                } else if (aMetaTileEntity instanceof MTEHatchDynamo) {
                    return this.mDynamoHatches.add((MTEHatchDynamo) aMetaTileEntity);
                } else if (aMetaTileEntity instanceof MTEHatchMaintenance) {
                    return this.mMaintenanceHatches.add((MTEHatchMaintenance) aMetaTileEntity);
                } else if (aMetaTileEntity instanceof MTEHatchDynamoMulti) {
                    return this.eDynamoMulti.add((MTEHatchDynamoMulti) aMetaTileEntity);
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public IStructureDefinition<MTEMultiNqGenerator> getStructure_EM() {
        if (multiDefinition == null) {
            multiDefinition = StructureDefinition.<MTEMultiNqGenerator>builder()
                .addShape(
                    mName,
                    transpose(
                        new String[][] {
                            { "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA" },
                            { "N     N", "       ", "  CCC  ", "  CPC  ", "  CCC  ", "       ", "N     N" },
                            { "N     N", "       ", "  CCC  ", "  CPC  ", "  CCC  ", "       ", "N     N" },
                            { "N     N", "       ", "  CCC  ", "  CPC  ", "  CCC  ", "       ", "N     N" },
                            { "N     N", "       ", "  CCC  ", "  CPC  ", "  CCC  ", "       ", "N     N" },
                            { "AAAAAAA", "A     A", "A CCC A", "A CPC A", "A CCC A", "A     A", "AAAAAAA" },
                            { "ANNNNNA", "N     N", "N CCC N", "N CPC N", "N CCC N", "N     N", "ANNNNNA" },
                            { "XXX~XXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX" }, }))
                .addElement(
                    'X',
                    ofChain(
                        buildHatchAdder(MTEMultiNqGenerator.class)
                            .atLeast(
                                tectech.thing.metaTileEntity.multi.base.TTMultiblockBase.HatchElement.DynamoMulti
                                    .or(gregtech.api.enums.HatchElement.Dynamo),
                                tectech.thing.metaTileEntity.multi.base.TTMultiblockBase.HatchElement.EnergyMulti
                                    .or(gregtech.api.enums.HatchElement.Energy),
                                gregtech.api.enums.HatchElement.InputHatch,
                                gregtech.api.enums.HatchElement.OutputHatch,
                                gregtech.api.enums.HatchElement.Maintenance)
                            .casingIndex(44)
                            .dot(1)
                            .build(),
                        ofBlock(GregTechAPI.sBlockCasings3, 12)))
                .addElement('A', ofBlock(GregTechAPI.sBlockCasings3, 12))
                .addElement('N', ofBlock(Loaders.radiationProtectionSteelFrame, 0))
                .addElement('C', ofBlock(Loaders.MAR_Casing, 0))
                .addElement('P', ofBlock(GregTechAPI.sBlockCasings2, 15))
                .build();
        }
        return multiDefinition;
    }

    public MTEMultiNqGenerator(String name) {
        super(name);
    }

    public MTEMultiNqGenerator(int id, String name, String nameRegional) {
        super(id, name, nameRegional);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        this.times = aNBT.getInteger("mTimes");
        this.leftEnergy = aNBT.getLong("mLeftEnergy");
        this.basicOutput = aNBT.getInteger("mbasicOutput");
        if (FluidRegistry.getFluid(aNBT.getString("mLockedFluidName")) != null) this.lockedFluid = new FluidStack(
            FluidRegistry.getFluid(aNBT.getString("mLockedFluidName")),
            aNBT.getInteger("mLockedFluidAmount"));
        else this.lockedFluid = null;
        super.loadNBTData(aNBT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("mTimes", this.times);
        aNBT.setLong("mLeftEnergy", this.leftEnergy);
        aNBT.setInteger("mbasicOutput", this.basicOutput);
        if (lockedFluid != null) {
            aNBT.setString(
                "mLockedFluidName",
                this.lockedFluid.getFluid()
                    .getName());
            aNBT.setInteger("mLockedFluidAmount", this.lockedFluid.amount);
        }
        super.saveNBTData(aNBT);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GoodGeneratorRecipeMaps.naquadahReactorFuels;
    }

    @Override
    protected boolean filtersFluid() {
        return false;
    }

    @Override
    public @NotNull CheckRecipeResult checkProcessing_EM() {

        ArrayList<FluidStack> tFluids = getStoredFluids();
        for (int i = 0; i < tFluids.size() - 1; i++) {
            for (int j = i + 1; j < tFluids.size(); j++) {
                if (GTUtility.areFluidsEqual(tFluids.get(i), tFluids.get(j))) {
                    if ((tFluids.get(i)).amount >= (tFluids.get(j)).amount) {
                        tFluids.remove(j--);
                    } else {
                        tFluids.remove(i--);
                        break;
                    }
                }
            }
        }

        GTRecipe tRecipe = GoodGeneratorRecipeMaps.naquadahReactorFuels.findRecipeQuery()
            .fluids(tFluids.toArray(new FluidStack[0]))
            .find();
        if (tRecipe != null) {
            Pair<FluidStack, Integer> excitedInfo = getExcited(tFluids.toArray(new FluidStack[0]), false);
            int pall = excitedInfo == null ? 1 : excitedInfo.getValue();
            if (consumeFuel(
                CrackRecipeAdder.copyFluidWithAmount(tRecipe.mFluidInputs[0], pall),
                tFluids.toArray(new FluidStack[0]))) {
                mOutputFluids = new FluidStack[] {
                    CrackRecipeAdder.copyFluidWithAmount(tRecipe.mFluidOutputs[0], pall) };
                basicOutput = tRecipe.mSpecialValue;
                times = pall;
                lockedFluid = excitedInfo == null ? null : excitedInfo.getKey();
                mMaxProgresstime = tRecipe.mDuration;
                return CheckRecipeResultRegistry.GENERATING;
            }
        }

        return CheckRecipeResultRegistry.NO_FUEL_FOUND;
    }

    @Override
    public boolean onRunningTick(ItemStack stack) {
        if (this.getBaseMetaTileEntity()
            .isServerSide()) {
            if (mMaxProgresstime != 0 && mProgresstime % 20 == 0) {
                // If there's no startRecipeProcessing, ME input hatch wouldn't work
                startRecipeProcessing();
                FluidStack[] input = getStoredFluids().toArray(new FluidStack[0]);
                int time = 1;
                if (LiquidAirConsumptionPerSecond != 0
                    && !consumeFuel(Materials.LiquidAir.getFluid(LiquidAirConsumptionPerSecond), input)) {
                    this.mEUt = 0;
                    this.trueEff = 0;
                    this.trueOutput = 0;
                    endRecipeProcessing();
                    return true;
                }
                int eff = consumeCoolant(input);
                if (consumeFuel(lockedFluid, input)) time = times;
                this.mEUt = basicOutput * eff * time / 100;
                this.trueEff = eff;
                this.trueOutput = (long) basicOutput * (long) eff * (long) time / 100;
                endRecipeProcessing();
            }
            addAutoEnergy(trueOutput);
        }
        return true;
    }

    @Override
    public String[] getInfoData() {
        String[] info = super.getInfoData();
        info[4] = StatCollector.translateToLocalFormatted(
            "gg.scanner.info.generator.generates",
            EnumChatFormatting.RED + GTUtility.formatNumbers(Math.abs(this.trueOutput)) + EnumChatFormatting.RESET);
        info[6] = StatCollector.translateToLocal("gg.scanner.info.generator.problems") + " "
            + EnumChatFormatting.RED
            + (this.getIdealStatus() - this.getRepairStatus())
            + EnumChatFormatting.RESET
            + " "
            + StatCollector.translateToLocal("gg.scanner.info.generator.efficiency")
            + " "
            + EnumChatFormatting.YELLOW
            + trueEff
            + EnumChatFormatting.RESET
            + " %";
        return info;
    }

    public boolean consumeFuel(FluidStack target, FluidStack[] input) {
        if (target == null) return false;
        for (FluidStack inFluid : input) {
            if (inFluid != null && inFluid.isFluidEqual(target) && inFluid.amount >= target.amount) {
                inFluid.amount -= target.amount;
                return true;
            }
        }
        return false;
    }

    public Pair<FluidStack, Integer> getExcited(FluidStack[] input, boolean isConsume) {
        for (Pair<FluidStack, Integer> fluidPair : excitedLiquid) {
            FluidStack tFluid = fluidPair.getKey();
            for (FluidStack inFluid : input) {
                if (inFluid != null && inFluid.isFluidEqual(tFluid) && inFluid.amount >= tFluid.amount) {
                    if (isConsume) inFluid.amount -= tFluid.amount;
                    return fluidPair;
                }
            }
        }
        return null;
    }

    /**
     * Finds valid coolant from given inputs and consumes if found.
     *
     * @param input Fluid inputs.
     * @return Efficiency of the coolant. 100 if not found.
     */
    private int consumeCoolant(FluidStack[] input) {
        for (Pair<FluidStack, Integer> fluidPair : coolant) {
            FluidStack tFluid = fluidPair.getKey();
            for (FluidStack inFluid : input) {
                if (inFluid != null && inFluid.isFluidEqual(tFluid) && inFluid.amount >= tFluid.amount) {
                    inFluid.amount -= tFluid.amount;
                    return fluidPair.getValue();
                }
            }
        }
        return 100;
    }

    public void addAutoEnergy(long outputPower) {
        if (!this.eDynamoMulti.isEmpty()) for (MTEHatch tHatch : this.eDynamoMulti) {
            long voltage = tHatch.maxEUOutput();
            long power = voltage * tHatch.maxAmperesOut();
            long outputAmperes;
            if (outputPower > power) doExplosion(8 * GTUtility.getTier(power));
            if (outputPower >= voltage) {
                leftEnergy += outputPower;
                outputAmperes = leftEnergy / voltage;
                leftEnergy -= outputAmperes * voltage;
                addEnergyOutput_EM(voltage, outputAmperes);
            } else {
                addEnergyOutput_EM(outputPower, 1);
            }
        }
        if (!this.mDynamoHatches.isEmpty()) for (MTEHatch tHatch : this.mDynamoHatches) {
            long voltage = tHatch.maxEUOutput();
            long power = voltage * tHatch.maxAmperesOut();
            long outputAmperes;
            if (outputPower > power) doExplosion(8 * GTUtility.getTier(power));
            if (outputPower >= voltage) {
                leftEnergy += outputPower;
                outputAmperes = leftEnergy / voltage;
                leftEnergy -= outputAmperes * voltage;
                addEnergyOutput_EM(voltage, outputAmperes);
            } else {
                addEnergyOutput_EM(outputPower, 1);
            }
        }
    }

    @Override
    public boolean checkMachine_EM(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        return structureCheck_EM(mName, 3, 7, 0) && mMaintenanceHatches.size() == 1
            && mDynamoHatches.size() + eDynamoMulti.size() == 1;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 0;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEMultiNqGenerator(this.mName);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        final MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Naquadah Reactor")
            .addInfo("Environmentally Friendly!")
            .addInfo("Generate power from high-energy liquids.")
            .addInfo(
                String.format(
                    "Consumes %d L/s Liquid Air to keep running, otherwise" + EnumChatFormatting.YELLOW
                        + " it will void your fuel"
                        + EnumChatFormatting.GRAY
                        + ".",
                    LiquidAirConsumptionPerSecond))
            .addInfo("Input liquid nuclear fuel or liquid naquadah fuel.")
            .addInfo(
                "The reactor will explode when there is more than" + EnumChatFormatting.RED
                    + " ONE"
                    + EnumChatFormatting.GRAY
                    + " type of fuel in hatches!")
            .addInfo("Can consume coolants to increase efficiency:")
            .addInfo(String.format("IC2 Coolant | %d%% | 1000 L/s ", CoolantEfficiency[3]))
            .addInfo(String.format("Super Coolant | %d%% | 1000 L/s", CoolantEfficiency[2]))
            .addInfo(String.format("Cryotheum | %d%% | 1000 L/s", CoolantEfficiency[1]))
            .addInfo(String.format("Tachyon Rich Temporal Fluid | %d%% | 20 L/s", CoolantEfficiency[0]))
            .addInfo("Can consume excited liquid to increase the output power and fuel usage:")
            .addInfo(String.format("Molten Caesium | %dx power | 180 L/s ", ExcitedLiquidCoe[4]))
            .addInfo(String.format("Molten Uranium-235 | %dx power | 180 L/s", ExcitedLiquidCoe[3]))
            .addInfo(String.format("Molten Naquadah | %dx power | 20 L/s", ExcitedLiquidCoe[2]))
            .addInfo(String.format("Molten Atomic Separation Catalyst | %dx power | 20 L/s", ExcitedLiquidCoe[1]))
            .addInfo(String.format("Spatially Enlarged Fluid | %dx power | 20 L/s", ExcitedLiquidCoe[0]))
            .addTecTechHatchInfo()
            .beginStructureBlock(7, 8, 7, true)
            .addController("Front bottom")
            .addCasingInfoExactly("Field Restriction Casing", 48, false)
            .addCasingInfoExactly("Radiation Proof Steel Frame Box", 36, false)
            .addCasingInfoExactly("Tungstensteel Pipe Casing", 6, false)
            .addCasingInfoExactly("Radiation Proof Machine Casing", 121, false)
            .addDynamoHatch("Any bottom layer casing, only accept ONE!")
            .addInputHatch("Any bottom layer casing")
            .addOutputHatch("Any bottom layer casing")
            .addMaintenanceHatch("Any bottom layer casing")
            .toolTipFinisher();
        return tt;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean aActive, boolean aRedstone) {
        if (side == facing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(44), TextureFactory.builder()
                .addIcon(Textures.BlockIcons.NAQUADAH_REACTOR_SOLID_FRONT_ACTIVE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(Textures.BlockIcons.NAQUADAH_REACTOR_SOLID_FRONT_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(44), TextureFactory.builder()
                .addIcon(Textures.BlockIcons.NAQUADAH_REACTOR_SOLID_FRONT)
                .extFacing()
                .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(44) };
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(mName, stackSize, 3, 7, 0, elementBudget, env, false, true);
    }

    @Override
    public boolean showRecipeTextInGUI() {
        return false;
    }
}
