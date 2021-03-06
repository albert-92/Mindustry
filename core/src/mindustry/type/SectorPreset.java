package mindustry.type;

import arc.*;
import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Objectives.*;
import mindustry.maps.generators.*;

import static mindustry.Vars.*;

//TODO ? remove ?
public class SectorPreset extends UnlockableContent{
    public @NonNull FileMapGenerator generator;
    public @NonNull Planet planet;
    public Array<Objectives.Objective> requirements = new Array<>();

    public Cons<Rules> rules = rules -> {};
    public boolean alwaysUnlocked;
    public int conditionWave = Integer.MAX_VALUE;
    public int launchPeriod = 10;
    public Schematic loadout = Loadouts.basicShard;

    protected Array<ItemStack> baseLaunchCost = new Array<>();
    protected Array<ItemStack> startingItems = new Array<>();
    protected Array<ItemStack> launchCost;
    protected Array<ItemStack> defaultStartingItems = new Array<>();

    public SectorPreset(String name, Planet planet, int sector){
        super(name);
        this.generator = new FileMapGenerator(name);
        this.planet = planet;

        planet.preset(sector, this);
    }

    //TODO
    /*
    public SectorPreset(String name){
        this(name, Planets.starter);
    }*/

    public Rules getRules(){
        return generator.map.rules();
    }

    public boolean isLaunchWave(int wave){
        return metCondition() && wave % launchPeriod == 0;
    }

    public boolean canUnlock(){
        return data.isUnlocked(this) || !requirements.contains(r -> !r.complete());
    }

    public Array<ItemStack> getLaunchCost(){
        if(launchCost == null){
            updateLaunchCost();
        }
        return launchCost;
    }

    public Array<ItemStack> getStartingItems(){
        return startingItems;
    }

    public void resetStartingItems(){
        startingItems.clear();
        defaultStartingItems.each(stack -> startingItems.add(new ItemStack(stack.item, stack.amount)));
    }

    public boolean hasLaunched(){
        return Core.settings.getBool(name + "-launched", false);
    }

    public void setLaunched(){
        updateObjectives(() -> {
            Core.settings.put(name + "-launched", true);
            data.modified();
        });
    }

    public void updateWave(int wave){
        int value = Core.settings.getInt(name + "-wave", 0);

        if(value < wave){
            updateObjectives(() -> {
                Core.settings.put(name + "-wave", wave);
                data.modified();
            });
        }
    }

    public void updateObjectives(Runnable closure){
        Array<ZoneObjective> incomplete = content.zones()
            .flatMap(z -> z.requirements)
            .select(o -> o.zone() == this && !o.complete())
            .as(ZoneObjective.class);

        closure.run();
        for(ZoneObjective objective : incomplete){
            if(objective.complete()){
                Events.fire(new ZoneRequireCompleteEvent(objective.zone, content.zones().find(z -> z.requirements.contains(objective)), objective));
            }
        }
    }

    public int bestWave(){
        return Core.settings.getInt(name + "-wave", 0);
    }

    /** @return whether initial conditions to launch are met. */
    public boolean isLaunchMet(){
        return bestWave() >= conditionWave;
    }

    public void updateLaunchCost(){
        Array<ItemStack> stacks = new Array<>();

        Cons<ItemStack> adder = stack -> {
            for(ItemStack other : stacks){
                if(other.item == stack.item){
                    other.amount += stack.amount;
                    return;
                }
            }
            stacks.add(new ItemStack(stack.item, stack.amount));
        };

        for(ItemStack stack : baseLaunchCost) adder.get(stack);
        for(ItemStack stack : startingItems) adder.get(stack);

        for(ItemStack stack : stacks){
            if(stack.amount < 0) stack.amount = 0;
        }

        stacks.sort();
        launchCost = stacks;
        Core.settings.putObject(name + "-starting-items", startingItems);
        data.modified();
    }

    /** Whether this zone has met its condition; if true, the player can leave. */
    public boolean metCondition(){
        //players can't leave in attack mode.
        return state.wave >= conditionWave && !state.rules.attackMode;
    }

    public boolean canConfigure(){
        return true;
    }

    @Override
    public void init(){

        for(ItemStack stack : startingItems){
            defaultStartingItems.add(new ItemStack(stack.item, stack.amount));
        }

        @SuppressWarnings("unchecked")
        Array<ItemStack> arr = Core.settings.getObject(name + "-starting-items", Array.class, () -> null);
        if(arr != null){
            startingItems = arr;
        }
    }

    @Override
    public boolean alwaysUnlocked(){
        return alwaysUnlocked;
    }

    @Override
    public boolean isHidden(){
        return true;
    }

    //neither of these are implemented, as zones are not displayed in a normal fashion... yet
    @Override
    public void displayInfo(Table table){
    }

    @Override
    public ContentType getContentType(){
        return ContentType.sector;
    }

}
