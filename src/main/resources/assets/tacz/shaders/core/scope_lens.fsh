#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;
uniform vec2 LensCenter;
uniform vec2 LensRadius;
uniform float Distortion;
uniform float Aberration;
uniform float EdgeWidth;
uniform vec3 LensTint;

out vec4 fragColor;

vec2 clampUv(vec2 uv) {
    vec2 halfTexel = 0.5 / ScreenSize;
    return clamp(uv, halfTexel, 1.0 - halfTexel);
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
    vec2 split = relative * Aberration * ramp;
    vec3 scene = vec3(
        texture(Sampler0, clampUv(sampleUv - split)).r,
        texture(Sampler0, clampUv(sampleUv)).g,
        texture(Sampler0, clampUv(sampleUv + split)).b
    );
    fragColor = vec4(scene * LensTint * (1.0 - edge), 1.0);
}
