package com.untamedears.realisticbiomes.listener;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Crops;
import org.bukkit.CropState;
import org.bukkit.ChatColor;
import com.untamedears.realisticbiomes.GrowthConfig;
import com.untamedears.realisticbiomes.GrowthMap;
import com.untamedears.realisticbiomes.RealisticBiomes;
import com.untamedears.realisticbiomes.persist.Plant;
import com.untamedears.realisticbiomes.utils.Fruits;
import com.untamedears.realisticbiomes.utils.MaterialAliases;
import java.util.ArrayList;

public class PlayerListener implements Listener {
	
	public static Logger LOG = Logger.getLogger("RealisticBiomes");
	
	private RealisticBiomes plugin;
	
	private GrowthMap growthConfigs;
	
	public PlayerListener(RealisticBiomes plugin, GrowthMap growthConfigs) {
		this.plugin = plugin;
		this.growthConfigs = growthConfigs;
	}
		
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event){
		Block block = event.getBlock();
		Material type = block.getType();
		MaterialData md = block.getState().getData();
		if(type.isSolid() == false){
			if(type.equals(Material.CROPS)){
				if(((Crops) md).getState() == CropState.RIPE){
					event.setCancelled(true);
					cropDrop(block, Material.SEEDS, true, true);
					cropDrop(block, Material.WHEAT, false, true);
					block.setType(Material.AIR);
				}
				return;
			} else if(type.equals(Material.BEETROOT_BLOCK)){ 
				if(((Crops) md).getState() == CropState.RIPE){
					event.setCancelled(true);
					cropDrop(block, Material.BEETROOT_SEEDS, true, true);
					cropDrop(block, Material.BEETROOT, false, true);
					block.setType(Material.AIR);
				}
				return;
			} else if(type.equals(Material.CARROT)){
				if(((Crops) md).getState() == CropState.RIPE){
					event.setCancelled(true);
					cropDrop(block, Material.CARROT_ITEM, true, false);
					block.setType(Material.AIR);
				}
				return;
			} else if(type.equals(Material.POTATO)){
				if(((Crops) md).getState() == CropState.RIPE){
					event.setCancelled(true);
					cropDrop(block, Material.POTATO_ITEM, true, false);
					block.setType(Material.AIR);
				}
				return;
			} else if(type.equals(Material.NETHER_WARTS)){
				if(block.getData() == 3){
					event.setCancelled(true);
					cropDrop(block, Material.NETHER_STALK, true, false);
					block.setType(Material.AIR);
				}
				return;
			} else if(type.equals(Material.COCOA)){
				if(block.getData() > 7 && block.getData() < 12){
					event.setCancelled(true);
					cropDrop(block, Material.INK_SACK, true, false);
					block.setType(Material.AIR);
				}
			}
		}
	}
	
	private void cropDrop(Block block, Material materialToDrop, boolean isSeed, boolean isTwoComponent){
		//I'm lazy so I'm hardcoding the base rates as 1.2 seeds after replant, and 1.2 of the desired crop (only counts once if seed = desired crop). So wheat would give 1.5 seeds on avg and 0.5 wheat. Carrots would give 1.5 carrots
		double biomeMultiplier = plugin.materialGrowth.get(block.getType()).getBiomeMultiplier(block.getBiome()); //nullpointerexception
		double surplusAvg = 1.2*(biomeMultiplier);
		int numToDrop = (int)surplusAvg; //integer portion
		if (Math.random() < surplusAvg - numToDrop){
			numToDrop++; //so if avg surplus is 7.1, it will always give you 7, and 10% of the time give you another one too.
		}
		if(isSeed){numToDrop++;} //one more for the guaranteed seed, so 8.1 on average gives 7.1 surplus
		ItemStack items;
		if(materialToDrop.equals(Material.INK_SACK)){
			items = new ItemStack(materialToDrop, numToDrop, (short)3);
		} else {
			items = new ItemStack(materialToDrop, numToDrop);
		}
        if(isSeed == false || isTwoComponent == false){
			ItemMeta meta = items.getItemMeta();
			ArrayList<String> lore = new ArrayList<String>(); //set lore to "Hand-Picked" only for fruits/vegetables themsleves, not dedicated seeds.
			lore.add("Ripe, Hand-Picked");
			meta.setLore(lore);
			items.setItemMeta(meta);
		}
        //LOG.info("RB debug: numToDrop is: " + numToDrop + " biomeMultiplier is: " + biomeMultiplier);
		block.getWorld().dropItemNaturally(block.getLocation(), items); 
    }
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		// right click block with the seeds or plant in hand to see what the status is
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getItem() == null) {
			return;
		}
		
		Plant plant = null;
		
		Block block = event.getClickedBlock();
		
		if (MaterialAliases.getBlockFromItem(event.getMaterial()) == block.getType()) {
			return;
		}
		
		GrowthConfig growthConfig;
		
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// hit the ground with a seed, or other farm product: get the adjusted crop growth
			// rate as if that crop was planted on top of the block
			
			growthConfig = MaterialAliases.getConfig(growthConfigs, event.getItem());
			if (growthConfig == null) {
				RealisticBiomes.doLog(Level.FINER, "No config found for \"" + event.getItem() + "\" : " + growthConfigs.keySet());
				return;
			}
			
			RealisticBiomes.doLog(Level.FINER, "LEFT CLICK: " + event.getItem() + ", " + growthConfig);
			
			if (event.getItem().getType() == Material.INK_SACK) {
				// if dye assume cocoa, otherwise would have exited earlier when growthConfig was null
				block = block.getRelative(event.getBlockFace());
			} else {
				block = block.getRelative(0,1,0);
			}
			
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
				&& (event.getItem().getType() == Material.STICK || event.getItem().getType() == Material.BONE)) {
			
			// right click on a growing crop with a stick: get information about that crop
			growthConfig = MaterialAliases.getConfig(growthConfigs, event.getClickedBlock());
			if (growthConfig == null) {
				return;
			}
			
			if (!Fruits.isFruit(event.getClickedBlock().getType())) {
				if (plugin.persistConfig.enabled && growthConfig != null && growthConfig.isPersistent()) {
					plant = plugin.growAndPersistBlock(block, false, growthConfig, null, null);
				}
			}
			
		} else {
			// right clicked without stick, bone, or plant item: do nothing
			return;
		}
		
		if (growthConfig.getType() == GrowthConfig.Type.FISHING_DROP) {
			return;
		}
		
		// show growth rate information if the item in the player's hand is not a bone
		if (event.getMaterial() == Material.BONE) {
			return;
		}
		
		String materialName = growthConfig.getName();

		if (plugin.persistConfig.enabled && growthConfig.isPersistent()) {
			double rate = growthConfig.getRate(block);
			RealisticBiomes.doLog(Level.FINER, "PlayerListener.onPlayerInteractEvent(): rate for " + materialName + " at block " + block + " is " + rate);
			rate = (1.0/(rate*(60.0*60.0/*seconds per hour*/)));
			RealisticBiomes.doLog(Level.FINER, "PlayerListener.onPlayerInteractEvent(): rate adjusted to "  + rate);
			
			if (plant == null) {
				String amount = new DecimalFormat("#0.00").format(rate);
				event.getPlayer().sendMessage("§7[Realistic Biomes] \"" + materialName + "\": "+amount+" hours to maturity");
				
			} else if (plant.getGrowth() == 1.0) {
				if (Fruits.isFruitFul(block.getType())) {
					
					if (!Fruits.hasFruit(block)) {
						Material fruitMaterial = Fruits.getFruit(event.getClickedBlock().getType());
						growthConfig = growthConfigs.get(fruitMaterial);
						if (growthConfig.isPersistent()) {
							block = Fruits.getFreeBlock(event.getClickedBlock(), null);
							if (block != null) {
								double fruitRate = growthConfig.getRate(block);
								RealisticBiomes.doLog(Level.FINER, "PlayerListener.onPlayerInteractEvent(): fruit rate for block " + block + " is " + fruitRate);
								fruitRate = (1.0/(fruitRate*(60.0*60.0/*seconds per hour*/)));
								RealisticBiomes.doLog(Level.FINER, "PlayerListener.onPlayerInteractEvent(): fruit rate adjusted to "  + fruitRate);
								
								String amount = new DecimalFormat("#0.00").format(fruitRate);
								String pAmount = new DecimalFormat("#0.00").format(fruitRate*(1.0-plant.getFruitGrowth()));
								event.getPlayer().sendMessage("§7[Realistic Biomes] \""+growthConfig.getName()+"\": "+pAmount+" of "+amount+" hours to maturity");
								return;
							}
						}
					}
					
				}
				
				RealisticBiomes.doLog(Level.FINER, "PlayerListener.onPlayerInteractEvent(): plant fruit is: " + plant.getFruitGrowth());
				String amount = new DecimalFormat("#0.00").format(rate);
				event.getPlayer().sendMessage("§7[Realistic Biomes] \"" + materialName + "\": "+amount+" hours to maturity");

			} else {
				
				RealisticBiomes.doLog(Level.FINER, "PlayerListener.onPlayerInteractEvent(): plant growth is: " + plant.getGrowth());
				String amount = new DecimalFormat("#0.00").format(rate);
				String pAmount = new DecimalFormat("#0.00").format(rate*(1.0-plant.getGrowth()));
				event.getPlayer().sendMessage("§7[Realistic Biomes] \"" + materialName + "\": "+pAmount+" of "+amount+" hours to maturity");
			}
			
		} else {
			// Persistence is not enabled
			double growthAmount = growthConfig.getRate(block);
			
			// clamp the growth value between 0 and 1 and put into percent format
			if (growthAmount > 1.0)
				growthAmount = 1.0;
			else if (growthAmount < 0.0)
				growthAmount = 0.0;
			String amount = new DecimalFormat("#0.00").format(growthAmount*100.0)+"%";
			
			String rateType;
			if (growthConfig.getType() == GrowthConfig.Type.ENTITY) {
				rateType = "Spawn rate";
			} else {
				rateType = "Growth rate";
			}
			// send the message out to the user!
			event.getPlayer().sendMessage("§7[Realistic Biomes] " + rateType + " \"" + materialName + "\" = "+amount);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (event.getPlayer().getItemInHand().getType() == Material.STICK) {
			Entity entity = event.getRightClicked();
			
			GrowthConfig growthConfig = growthConfigs.get(entity.getType());
			if (growthConfig == null)
				return;
			
			double growthAmount = growthConfig.getRate(entity.getLocation().getBlock());
			
			// clamp the growth value between 0 and 1 and put into percent format
			if (growthAmount > 1.0)
				growthAmount = 1.0;
			else if (growthAmount < 0.0)
				growthAmount = 0.0;
			String amount = new DecimalFormat("#0.00").format(growthAmount*100.0)+"%";
			// send the message out to the user!
			event.getPlayer().sendMessage("§7[Realistic Biomes] Spawn rate \""+growthConfig.getName()+"\" = "+amount);
		}
	}
}
