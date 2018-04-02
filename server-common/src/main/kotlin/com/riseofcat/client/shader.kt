package com.riseofcat.client

//language=GLSL
const val shader_default_vertex_shader_vert = """
attribute vec4 a_position; //позиция вершины
attribute vec4 a_color; //цвет вершины
attribute vec2 a_texCoord0; //координаты текстуры
uniform mat4 u_projTrans;  //матрица, которая содержит данные для преобразования проекции и вида
varying vec4 v_color;  //цвет который будет передан в фрагментный шейдер
varying vec4 vColor;  //цвет который будет передан в фрагментный шейдер
varying vec2 v_texCoords;  //координаты текстуры
varying vec2 v_texCoord;  //координаты текстуры
varying vec2 vTexCoord;  //координаты текстуры
void main(){
    v_color=a_color;
    vColor = a_color;
    // При передаче цвет из SpriteBatch в шейдер, происходит преобразование из ABGR int цвета в float.
    // что-бы избежать NAN  при преобразование, доступен не весь диапазон для альфы, а только значения от (0-254)
    //чтобы полностью передать непрозрачность цвета, когда альфа во float равна 1, то всю альфу приходится умножать.
    //это специфика libgdx и о ней надо помнить при переопределение  вершинного шейдера.
    v_color.a = v_color.a * (255.0/254.0);//todo
    v_texCoords = a_texCoord0;
    v_texCoord = a_texCoord0;
    vTexCoord = a_texCoord0;
    //применяем преобразование вида и проекции, можно не забивать себе этим голову
    // тут происходят математические преобразование что-бы правильно учесть параметры камеры
    // gl_Position это окончательная позиция вершины
    gl_Position =  u_projTrans * a_position;
}
"""
//В шейдерах писать только ASCII символы //todo сделать extension String.removeNonAscii() для удаления не ASCII символов
//language=GLSL
const val shader_background_stars_frag = """
#version 100
//todo на linux yoga нельзя выставить 101. Узнать разницу API между 100 и 101
#ifdef GL_ES
    precision highp float;//todo uniform mouse не дает выставить общую точность lowp и даже mediump
    precision lowp int;
#else
#define highp;//todo test on non GL ES devices
#endif
uniform mediump float time;
uniform vec2 resolution;// = vec2(400.0, 400.0);//todo int? lowp?? протестировать на больших экранах
uniform /*highp*/ vec2 mouse;// = vec2(0.0,0.0);
#define VOLSTEPS 2//количество слоёв
//От 0.2 до 1.0
//Важный параметр
#define STEPSIZE 0.65
#define ZOOM   5.900
#define TILE   0.850
#define SPEED  0.150
#define SATURATION 0.750
//Может быть как положительный так и отрицательный целые значения увеличивают выпад пятен
#define INTERESTING1 2.2

// Component wise blending
#define Blend(base, blend, funcf)       vec3(funcf(base.r, blend.r), funcf(base.g, blend.g), funcf(base.b, blend.b))
// Blend Funcs
#define BlendScreenf(base, blend)       (1.0 - ((1.0 - base) * (1.0 - blend)))
#define BlendMultiply(base, blend)       (base * blend)
#define BlendScreen(base, blend)       Blend(base, blend, BlendScreenf)

void mainImage( out vec3 fragColor, in vec2 fragCoord )
{
	//get coords and direction
	vec2 uv= fragCoord.xy/resolution.xy-.5;
	uv.y*=resolution.y/resolution.x;
	vec3 dir=vec3(uv*ZOOM,1.1);
	float time=time*SPEED+.25;
	time = time*1.0;
	float a1=.5+time/resolution.x*2.;
	float a2=.8+time/resolution.y*2.;
	mat2 rot1=mat2(cos(a1),sin(a1),-sin(a1),cos(a1));
	mat2 rot2=mat2(cos(a2),sin(a2),-sin(a2),cos(a2));
	dir.xz*=rot1;
	dir.xy*=rot2;
	vec3 from=vec3(0.9,.5,0.5);
	from+=vec3(mouse.x*5., mouse.y*5.,INTERESTING1);//Направление движения
	from.xz*=rot1;
	from.xy*=rot2;

	//volumetric rendering
	float s=0.1 + STEPSIZE,fade=1.1;
	vec3 v=vec3(0.);
	for (int r=0; r<VOLSTEPS; r++) {
		vec3 p=from+s*dir*.5;
		p = abs(vec3(TILE)-mod(p,vec3(TILE*2.))); // tiling fold
		float a=0.;
		float pa = 6.0;
		float param = 0.8;
		if(r == 0) {
		    param = 1.2 * (1.0 + sin(time*3.1)*0.05);
		}
		if(r == 1) {
		    param = 0.8 * (1.0 + sin(time)*0.02);
		}
        int limitI = 2 + 2*(VOLSTEPS - r - 1);
        if(r==0) {
            for (int i1=0; i1< 4; i1++) { //Качество
                p=1.4*abs(p)/dot(p,p)-param; // the magic formula
                a+=1.0 * abs(length(p)-pa); // absolute sum of average change
                pa*=0.65;//length(p);
            }
        } else {
            for (int i2=0; i2< 2; i2++) { //Качество
                p=1.4*abs(p)/dot(p,p)-param; // the magic formula
                a+=1.0 * abs(length(p)-pa); // absolute sum of average change
                pa*=0.65;//length(p);
            }
        }
		a*=sqrt(sqrt(a))*(1.0 + sqrt(sqrt(a))*float(r)); // add contrast
		v+=fade;
		v+=vec3(sin(s/0.01999)*0.5*exp(s),s,0.25*exp(s))*a*fade * 0.770; // COLOR based on distance
		fade*= a * 0.008; // distance fading
		s+=STEPSIZE;
	}
	v=mix(vec3(length(v)),v,SATURATION); //color adjust
	fragColor = v*.009;
}
float snoise(vec3 uv, float res)
{
   const vec3 s = vec3(1e0, 1e2, 1e4);
   uv *= res;
   vec3 uv0 = floor(mod(uv, res))*s;
   vec3 uv1 = floor(mod(uv+vec3(1.), res))*s;
   vec3 f = fract(uv); f = f*f*f*(3.0-2.0*f);
   vec4 v = vec4(uv0.x+uv0.y+uv0.z, uv1.x+uv0.y+uv0.z,uv0.x+uv1.y+uv0.z, uv1.x+uv1.y+uv0.z);
   vec4 r = fract(sin(v*1e-3)*1e5);
   float r0 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
   r = fract(sin((v + uv1.z - uv0.z)*1e-3)*1e5);
   float r1 = mix(mix(r.x, r.y, f.x), mix(r.z, r.w, f.x), f.y);
   return mix(r0, r1, f.z)*2.-0.3;
}
vec3 getrainbow(vec2 coords,float intensity)
{
	coords = coords/6000.0;
	return normalize(vec3(snoise(vec3(coords,0.0),intensity),
                  snoise(vec3(coords,0.1),intensity),
                  snoise(vec3(coords,0.2),intensity)   ));
}
void main (void)
{
  vec3 color = vec3 (0.0, 0.0, 0.0);
  mainImage (color, gl_FragCoord.xy);
  //color.r = sqrt(sqrt(color.r));
  //color.g = sqrt(sqrt(color.g));
  //color.b = sqrt(color.b);
  //color = color*0.23;
  //color.w = 1.0;
  vec2 p = 2.0 * (gl_FragCoord.xy / resolution) - 1.0 ;
  p.x *= resolution.x/resolution.y;
  p *= 500.0;
  float dx = mouse.x*30.0;
  float dy = mouse.y*30.0;
  vec2 offset = vec2(dx*cos(dx/100.0),dy*sin(dy/100.0));
  //vec3 rainbow = getrainbow(p.xy+offset*30.0,11.0)*0.1;
  //gl_FragColor = vec4(rainbow, 1.0);

  gl_FragColor = vec4(color, 1.0);//1
  //float r = 1.0 - (1.0 - color.r) * (1.0 - rainbow.r);//2
  //float g = 1.0 - (1.0 - color.g) * (1.0 - rainbow.g);//2
  //float b = 1.0 - (1.0 - color.b) * (1.0 - rainbow.b);//2
  //gl_FragColor = vec4(r, g, b, 1.0);//2

  //gl_FragColor = vec4(BlendScreen(color.rgb, rainbow), 1.0);//test or not
}
"""
//language=GLSL
const val shader_good_blur_frag="""
#ifdef GL_ES
precision mediump float;
precision mediump int;
#else
#define highp;
#endif

varying vec4 v_color;
varying vec2 v_texCoord;

uniform sampler2D u_texture;
/*uniform*/ float resolution = 800.0;//width
/*uniform*/ float radius = 5.0;
/*uniform*/ vec2 dir = vec2(1.0, 0.0);//Этот шейдер расчитан на то чтобы сначала рендерить в один frame buffer с блюром по горизонтально например (dir = vec2(1.0,0.0)), а потом этот frame buffer рендерить вертикально dir=vec2(0.0,1.0)

void main() {
	vec4 sum = vec4(0.0);
	vec2 tc = v_texCoord;

	// Number of pixels off the central pixel to sample from
	float blur = radius/resolution;

	// Blur direction
	float hstep = dir.x;
	float vstep = dir.y;

	// Apply blur using 9 samples and predefined gaussian weights
	//Сумма коэффициентов наверное равна 1
	sum += texture2D(u_texture, vec2(tc.x - 4.0*blur*hstep, tc.y - 4.0*blur*vstep)) * 0.006;
	sum += texture2D(u_texture, vec2(tc.x - 3.0*blur*hstep, tc.y - 3.0*blur*vstep)) * 0.044;
	sum += texture2D(u_texture, vec2(tc.x - 2.0*blur*hstep, tc.y - 2.0*blur*vstep)) * 0.121;
	sum += texture2D(u_texture, vec2(tc.x - 1.0*blur*hstep, tc.y - 1.0*blur*vstep)) * 0.194;

	sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.27;

	sum += texture2D(u_texture, vec2(tc.x + 1.0*blur*hstep, tc.y + 1.0*blur*vstep)) * 0.194;
	sum += texture2D(u_texture, vec2(tc.x + 2.0*blur*hstep, tc.y + 2.0*blur*vstep)) * 0.121;
	sum += texture2D(u_texture, vec2(tc.x + 3.0*blur*hstep, tc.y + 3.0*blur*vstep)) * 0.044;
	sum += texture2D(u_texture, vec2(tc.x + 4.0*blur*hstep, tc.y + 4.0*blur*vstep)) * 0.006;

	gl_FragColor = sum;
}
"""
//language=GLSL
const val shader_mesh_default_vert= """
attribute vec2 aVertexPosition;

void main(void) {
    gl_Position = vec4(aVertexPosition, 0.0, 1.0);
}
"""

