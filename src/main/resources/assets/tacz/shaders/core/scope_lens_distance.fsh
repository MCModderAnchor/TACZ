#version 150

uniform sampler2D Sampler0;
uniform vec2 MapSize;
uniform vec2 PixelScale;
uniform int JumpStep;

out vec4 fragColor;

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    vec4 best = vec4(0.0);
    float bestDistance = 1e30;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            ivec2 neighbour = pixel + ivec2(x, y) * JumpStep;
            if (any(lessThan(neighbour, ivec2(0))) || any(greaterThanEqual(neighbour, ivec2(MapSize)))) continue;
            vec4 encoded = texelFetch(Sampler0, neighbour, 0);
            vec4 bytes = floor(encoded * 255.0 + 0.5);
            vec2 seed = vec2(bytes.r * 256.0 + bytes.g, bytes.b * 256.0 + bytes.a) - 1.0;
            if (seed.x < 0.0) continue;
            vec2 delta = (seed - vec2(pixel)) * PixelScale;
            float distanceSquared = dot(delta, delta);
            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                best = encoded;
            }
        }
    }
    fragColor = best;
}
