package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

public class ItemPole extends ItemPoleComponent implements IItemBlock{
	
	public ItemPole(JSONPoleComponent definition){
		super(definition);
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockPole.class;
	}
}
