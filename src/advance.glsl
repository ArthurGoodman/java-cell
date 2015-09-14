
uniform sampler2D texture;

uniform float width;
uniform float height;

float deltaX = 1.0 / width;
float deltaY = 1.0 / height;

uniform int init;
uniform float time;

#extension GL_EXT_gpu_shader4 : enable
#extension GL_ARB_shader_bit_encoding : enable

// A single iteration of Bob Jenkins' One-At-A-Time hashing algorithm.
uint hash(uint x) {
    x += (x << 10u);
    x ^= (x >> 6u);
    x += (x << 3u);
    x ^= (x >> 11u);
    x += (x << 15u);
    return x;
}

// Compound versions of the hashing algorithm I whipped together.
uint hash(uvec2 v) { return hash(v.x ^ hash(v.y)); }
uint hash(uvec3 v) { return hash(v.x ^ hash(v.y) ^ hash(v.z)); }
uint hash(uvec4 v) { return hash(v.x ^ hash(v.y) ^ hash(v.z) ^ hash(v.w)); }

// Construct a float with half-open range [0:1] using low 23 bits.
// All zeroes yields 0.0, all ones yields the next smallest representable value below 1.0.
float floatConstruct(uint m) {
    const uint ieeeMantissa = 0x007FFFFFu; // binary32 mantissa bitmask
    const uint ieeeOne = 0x3F800000u; // 1.0 in IEEE binary32

    m &= ieeeMantissa; // Keep only mantissa bits (fractional part)
    m |= ieeeOne; // Add fractional part to 1.0

    float f = uintBitsToFloat(m); // Range [1:2]
    return f - 1.0; // Range [0:1]
}

// Pseudo-random value in half-open range [0:1].
float random(vec3 v) { return floatConstruct(hash(floatBitsToUint(v))); }

float rand() {
	return random(vec3(gl_TexCoord[0].st, time));
}

int cell(int x, int y) {
    return int(texture2D(texture, gl_TexCoord[0].st + vec2(float(x) * deltaX, float(y) * deltaY)).r * 255.0 + 0.5);
}

int x() {
	return int(gl_TexCoord[0].x);
}

int y() {
	return int(gl_TexCoord[0].y);
}

int at(int x, int y) {
	return int(texture2D(texture, vec2(float(x) * deltaX, float(y) * deltaY)).r * 255.0 + 0.5);
}

vec4 hsvToRgb(float h, float s, float v) { return vec4(0.0, 0.0, 0.0, 0.0); }

//int neumann() {
//	return cell(1, 1) + cell(-1, 1) + cell(-1, -1) + cell(1, -1) + cell(0, 1) + cell(0, -1) + cell(-1, 0) + cell(1, 0);
//}

//int life() {
//	int n = neumann(), c = cell(0, 0), r = c;
	
//	if(n == 3) r = 1;
//	else if(c == 1 && n != 2) r = 0;
	
//	return r;
//}

//void main() {
//    if(init == 1) {
//        gl_FragColor = vec4(float(int(random(vec3(gl_TexCoord[0].st, time)) + 0.5)) / 255.0, 0.0, 0.0, 0.0);
        //if(gl_TexCoord[0].t*width > width/2 - 0.5 && gl_TexCoord[0].t*width < width/2 + 0.5) gl_FragColor = vec4(0.9, 0.9, 0.9, 1.0);
        //else gl_FragColor = vec4(0.1, 0.1, 0.1, 1.0);
//    }
//    else gl_FragColor = vec4(float(life()) / 255.0, 0.0, 0.0, 0.0);
//}
