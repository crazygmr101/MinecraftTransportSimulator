package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;

public abstract class APartEngineGeared extends APartEngine{
	public byte currentGear;

	public APartEngineGeared(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		this.currentGear = dataTag.getByte("gearNumber");
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		//Do automatic transmission functions if needed.
		if(state.running && pack.engine.isAutomatic){
			if(currentGear > 0){
				if(RPM > getSafeRPMFromMax(this.pack.engine.maxRPM)*0.5F*(1.0F + vehicle.throttle/100F)){
					shiftUp(false);
				}else if(RPM < getSafeRPMFromMax(this.pack.engine.maxRPM)*0.25*(1.0F + vehicle.throttle/100F) && currentGear > 1){
					shiftDown(false);
				}
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = super.getPartNBTTag();
		dataTag.setByte("gearNumber", this.currentGear);
		return dataTag;
	}
	
	protected float getRatioForCurrentGear(){
		return currentGear == -1 ? pack.engine.gearRatios[0] : currentGear > 0 ? pack.engine.gearRatios[currentGear + 1] : 0;
	}
	
	public float getGearshiftRotation(){
		return pack.engine.isAutomatic ? Math.min(1, currentGear)*15F : currentGear*5;
	}
	
	public void shiftUp(boolean packet){
		if(currentGear == -1){
			currentGear = 0;
		}else if(currentGear == 0){
			if(vehicle.velocity > -0.1){
				currentGear = 1;
			}else if(vehicle.world.isRemote){
				MTS.proxy.playSound(partPos, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1);
			}
		}else if(currentGear < pack.engine.gearRatios.length - 2){
			if(pack.engine.isAutomatic && packet){
				currentGear = 1;
			}else{
				++currentGear;
			}
		}
	}
	
	public void shiftDown(boolean packet){
		if(currentGear > 0){
			if(pack.engine.isAutomatic && packet){
				currentGear = 0;
			}else{
				--currentGear;
			}
		}else if(currentGear == 0){
			if(vehicle.velocity < 0.1){
				currentGear = -1;
			}else if(vehicle.world.isRemote){
				MTS.proxy.playSound(partPos, MTS.MODID + ":engine_shifting_grinding", 1.0F, 1);
			}
		}
	}
}