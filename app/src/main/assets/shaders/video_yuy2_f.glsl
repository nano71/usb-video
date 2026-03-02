#version 300 es
precision mediump float;
in vec2 vTexCoord;
out vec4 fragColor;
uniform sampler2D uTextureYUV;
uniform float uTime;
uniform int uShowZebra;

void main() {
    vec4 packedYUV = texture(uTextureYUV, vTexCoord);

    float y0 = packedYUV.r;
    float u  = packedYUV.g - 0.5;
    float y1 = packedYUV.b;
    float v  = packedYUV.a - 0.5;

    float width = float(textureSize(uTextureYUV, 0).x) * 2.0;
    float xPos = vTexCoord.x * width;
    float y = (mod(xPos, 2.0) < 1.0) ? y0 : y1;

    float r = y + 1.402 * v;
    float g = y - 0.34414 * u - 0.71414 * v;
    float b = y + 1.772 * u;

    vec4 color = vec4(r, g, b, 1.0);

    if (uShowZebra == 1) {
        float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        float stripe = mod((gl_FragCoord.x - gl_FragCoord.y + uTime * 0.05), 16.0);
        if (stripe < 6.0) {
            if (luma >= 0.85) {
                color = vec4(1.0, 0.0, 0.0, 1.0);
            } else if (luma >= 0.82) {
                color = vec4(0.0, 1.0, 0.0, 1.0);
            }
        }
    }
    fragColor = color;
}
