#version 150

uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    fragColor = vertexColor;
}
