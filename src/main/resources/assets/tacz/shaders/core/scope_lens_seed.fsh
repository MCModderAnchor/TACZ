#version 150

uniform sampler2D Sampler0;
uniform vec2 SourceSize;
uniform vec2 MapSize;
uniform vec4 MaskRect;

out vec4 fragColor;

void main() {
    vec2 pixel = floor(gl_FragCoord.xy);
    vec2 source = MaskRect.xy + (pixel + 0.5) / MapSize * MaskRect.zw;
    float mask = texelFetch(Sampler0, ivec2(clamp(source, vec2(0.0), SourceSize - 1.0)), 0).r;
    // Exterior texels seed the distance transform. Zero encodes an unassigned interior texel.
    vec2 coordinate = pixel + 1.0;
    vec2 high = floor(coordinate / 256.0);
    vec2 low = mod(coordinate, 256.0);
    fragColor = mask < 0.5 ? vec4(high.x, low.x, high.y, low.y) / 255.0 : vec4(0.0);
}
