#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 ScreenSize;
uniform vec2 LensCenter;
uniform vec2 LensRadius;
uniform float Distortion;
uniform float Aberration;
uniform float EdgeWidth;
uniform vec3 LensTint;
uniform int VisionMode;
uniform int ThermalReady;
uniform float VisionGain;
uniform float VisionNoise;
uniform float SensorTime;

out vec4 fragColor;

vec2 clampUv(vec2 uv) {
    vec2 halfTexel = 0.5 / ScreenSize;
    return clamp(uv, halfTexel, 1.0 - halfTexel);
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float grain() {
    vec2 pixel = floor(gl_FragCoord.xy);
    float frame = floor(SensorTime * 30.0);
    return fract(sin(dot(pixel, vec2(12.9898, 78.233)) + frame * 37.719) * 43758.5453) - 0.5;
}

vec3 ironbow(float heat) {
    vec3 cold = vec3(0.015, 0.01, 0.06);
    vec3 purple = vec3(0.30, 0.025, 0.48);
    vec3 red = vec3(0.90, 0.10, 0.035);
    vec3 yellow = vec3(1.0, 0.82, 0.12);
    if (heat < 0.35) return mix(cold, purple, heat / 0.35);
    if (heat < 0.65) return mix(purple, red, (heat - 0.35) / 0.30);
    if (heat < 0.85) return mix(red, yellow, (heat - 0.65) / 0.20);
    return mix(yellow, vec3(1.0), (heat - 0.85) / 0.15);
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec2 relative = uv - LensCenter;
    float distance = clamp(length(relative / max(LensRadius, vec2(0.00001))), 0.0, 1.0);

    float ramp = 0.0;
    float edge = 0.0;
    if (EdgeWidth > 0.0) {
        float width = 0.155 * EdgeWidth;
        float falloff = (1.0 - distance) / width;
        edge = exp(-falloff * falloff);
        ramp = smoothstep(max(1.0 - width * 3.0, 0.0), 1.0, distance);
    }

    // Sample in screen space so hand FOV cannot magnify the scene a second time.
    vec2 sampleUv = uv - relative * Distortion * ramp;
    // Digital sensors are monochrome before their display palette is applied.
    bool digital = VisionMode == 1 || VisionMode == 2 || (VisionMode >= 3 && ThermalReady != 0);
    vec2 split = digital ? vec2(0.0) : relative * Aberration * ramp;
    vec3 scene = vec3(
        texture(Sampler0, clampUv(sampleUv - split)).r,
        texture(Sampler0, clampUv(sampleUv)).g,
        texture(Sampler0, clampUv(sampleUv + split)).b
    );
    vec3 color = scene * LensTint;
    if (VisionMode == 1 || VisionMode == 2) {
        float signal = 1.0 - exp(-pow(max(luminance(scene), 0.0), 0.65) * VisionGain);
        signal = clamp(signal + grain() * VisionNoise, 0.0, 1.0);
        vec3 phosphor = VisionMode == 1 ? vec3(0.18, 1.0, 0.28) : vec3(0.82, 0.94, 1.0);
        color = phosphor * signal;
    } else if (VisionMode >= 3 && ThermalReady != 0) {
        float detail = clamp(luminance(scene), 0.0, 1.0);
        float target = texture(Sampler1, clampUv(sampleUv)).r;
        float heat = mix(0.06 + detail * 0.24, 0.78 + detail * 0.20, target);
        heat = clamp(heat + grain() * VisionNoise, 0.0, 1.0);
        if (VisionMode == 3) color = vec3(heat);
        else if (VisionMode == 4) color = vec3(1.0 - heat);
        else color = ironbow(heat);
    }
    fragColor = vec4(color * (1.0 - edge), 1.0);
}
