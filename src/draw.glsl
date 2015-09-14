
uniform sampler2D texture;

vec4 hsvToRgb(float h, float s, float v)
{
    float a = 1.0;
	float c = v * s;
	h = mod((h * 6.0), 6.0);
	float x = c * (1.0 - abs(mod(h, 2.0) - 1.0));
	vec4 color;
 
	if (0.0 <= h && h < 1.0) {
		color = vec4(c, x, 0.0, a);
	} else if (1.0 <= h && h < 2.0) {
		color = vec4(x, c, 0.0, a);
	} else if (2.0 <= h && h < 3.0) {
		color = vec4(0.0, c, x, a);
	} else if (3.0 <= h && h < 4.0) {
		color = vec4(0.0, x, c, a);
	} else if (4.0 <= h && h < 5.0) {
		color = vec4(x, 0.0, c, a);
	} else if (5.0 <= h && h < 6.0) {
		color = vec4(c, 0.0, x, a);
	} else {
		color = vec4(0.0, 0.0, 0.0, a);
	}
 
	color.rgb += v - c;
 
	return color;
}

float rand() { return 0.0; }
int cell(int x, int y) { return 0; }
int x() { return 0; }
int y() { return 0; }
int at(int x, int y) { return 0; }

//float cell() {
//	return texture2D(texture, gl_TexCoord[0].st).r * 255.0;
//}

//void main() {
//	if(cell() > 0.5) gl_FragColor = vec4(0.9, 0.9, 0.9, 1.0);
//	else gl_FragColor = vec4(0.1, 0.1, 0.1, 1.0);
//}
