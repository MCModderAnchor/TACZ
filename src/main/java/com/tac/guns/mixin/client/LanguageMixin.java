package com.tac.guns.mixin.client;

import com.tac.guns.client.resource.ClientAssetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ClientLanguage.class)
public class LanguageMixin {
    @Inject(method = "getOrDefault(Ljava/lang/String;)Ljava/lang/String;", at = @At(value = "HEAD"), cancellable = true)
    public void getCustomLanguage(String key, CallbackInfoReturnable<String> call) {
        LanguageInfo language = Minecraft.getInstance().getLanguageManager().getSelected();
        String code = language.getCode();
        Map<String, String> languages = ClientAssetManager.INSTANCE.getLanguages(code);
        Map<String, String> alternative = ClientAssetManager.INSTANCE.getLanguages("en_us");
        if (languages != null && languages.containsKey(key)) {
            call.setReturnValue(languages.get(key));
        } else if (alternative != null && alternative.containsKey(key)) {
            call.setReturnValue(alternative.get(key));
        }
    }

    @Inject(method = "has(Ljava/lang/String;)Z", at = @At(value = "HEAD"), cancellable = true)
    public void hasCustomLanguage(String key, CallbackInfoReturnable<Boolean> call) {
        LanguageInfo language = Minecraft.getInstance().getLanguageManager().getSelected();
        String code = language.getCode();
        Map<String, String> languages = ClientAssetManager.INSTANCE.getLanguages(code);
        Map<String, String> alternative = ClientAssetManager.INSTANCE.getLanguages("en_us");
        if (languages != null && languages.containsKey(key)) {
            call.setReturnValue(true);
        } else if (alternative != null && alternative.containsKey(key)) {
            call.setReturnValue(true);
        }
    }
}
