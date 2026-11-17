package com.ecologies.client;

//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?}

//? if >=1.21.2 {
public class MuskoxRenderState extends LivingEntityRenderState {
    public int variant;
    public float headDown, nibble, ram;
}
//?} else {
/*/^* Render states don't exist before 1.21.2; this placeholder keeps the file compiling. *^/
public final class MuskoxRenderState {
    private MuskoxRenderState() {}
}
*///?}
