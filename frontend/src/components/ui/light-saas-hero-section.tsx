import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Lottie } from "lottie-react";
import { IndianRupee, PlayCircle, ShieldCheck, Sparkles } from "lucide-react";
import { Renderer, Program, Mesh, Triangle } from "ogl";

interface PlasmaProps {
  color?: string;
  speed?: number;
  direction?: "forward" | "reverse" | "pingpong";
  scale?: number;
  opacity?: number;
  mouseInteractive?: boolean;
}

const hexToRgb = (hex: string): [number, number, number] => {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!result) return [0.15, 0.39, 0.92];
  return [
    parseInt(result[1], 16) / 255,
    parseInt(result[2], 16) / 255,
    parseInt(result[3], 16) / 255,
  ];
};

const vertex = `#version 300 es
precision highp float;
in vec2 position;
in vec2 uv;
out vec2 vUv;
void main() {
  vUv = uv;
  gl_Position = vec4(position, 0.0, 1.0);
}
`;

const fragment = `#version 300 es
precision highp float;
uniform vec2 iResolution;
uniform float iTime;
uniform vec3 uCustomColor;
uniform float uUseCustomColor;
uniform float uSpeed;
uniform float uDirection;
uniform float uScale;
uniform float uOpacity;
uniform vec2 uMouse;
uniform float uMouseInteractive;
out vec4 fragColor;

void mainImage(out vec4 o, vec2 C) {
  vec2 center = iResolution.xy * 0.5;
  C = (C - center) / uScale + center;

  vec2 mouseOffset = (uMouse - center) * 0.0002;
  C += mouseOffset * length(C - center) * step(0.5, uMouseInteractive);

  float i, d, z, T = iTime * uSpeed * uDirection;
  vec3 O, p, S;

  for (vec2 r = iResolution.xy, Q; ++i < 60.; O += o.w/d*o.xyz) {
    p = z*normalize(vec3(C-.5*r,r.y));
    p.z -= 4.;
    S = p;
    d = p.y-T;

    p.x += .4*(1.+p.y)*sin(d + p.x*0.1)*cos(.34*d + p.x*0.05);
    Q = p.xz *= mat2(cos(p.y+vec4(0,11,33,0)-T));
    z+= d = abs(sqrt(length(Q*Q)) - .25*(5.+S.y))/3.+8e-4;
    o = 1.+sin(S.y+p.z*.5+S.z-length(S-p)+vec4(2,1,0,8));
  }

  o.xyz = tanh(O/1e4);
}

bool finite1(float x){ return !(isnan(x) || isinf(x)); }
vec3 sanitize(vec3 c){
  return vec3(
    finite1(c.r) ? c.r : 0.0,
    finite1(c.g) ? c.g : 0.0,
    finite1(c.b) ? c.b : 0.0
  );
}

void main() {
  vec4 o = vec4(0.0);
  mainImage(o, gl_FragCoord.xy);
  vec3 rgb = sanitize(o.rgb);

  float intensity = (rgb.r + rgb.g + rgb.b) / 3.0;
  vec3 customColor = intensity * uCustomColor;
  vec3 finalColor = mix(rgb, customColor, step(0.5, uUseCustomColor));

  float alpha = length(rgb) * uOpacity;
  fragColor = vec4(finalColor, alpha);
}`;

function canUseWebGL2(): boolean {
  try {
    const test = document.createElement("canvas");
    return test.getContext("webgl2", { alpha: true }) !== null;
  } catch {
    return false;
  }
}

export const Plasma = ({
  color = "#ffffff",
  speed = 1,
  direction = "forward",
  scale = 1,
  opacity = 1,
  mouseInteractive = true,
}: PlasmaProps) => {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mousePos = useRef({ x: 0, y: 0 });
  const [useFallback, setUseFallback] = useState(() => !canUseWebGL2());

  useEffect(() => {
    const container = containerRef.current;
    if (!container || useFallback) return;

    let renderer: Renderer | null = null;
    let raf = 0;
    let ro: ResizeObserver | null = null;
    let canvas: HTMLCanvasElement | null = null;

    try {
      const useCustomColor = color ? 1.0 : 0.0;
      const customColorRgb = color ? hexToRgb(color) : [1, 1, 1];
      const directionMultiplier = direction === "reverse" ? -1.0 : 1.0;

      renderer = new Renderer({
        webgl: 2,
        alpha: true,
        antialias: false,
        dpr: Math.min(window.devicePixelRatio || 1, 2),
      });

      const gl = renderer.gl;
      if (!gl) {
        throw new Error("WebGL unavailable");
      }

      canvas = gl.canvas as HTMLCanvasElement;
      canvas.style.display = "block";
      canvas.style.width = "100%";
      canvas.style.height = "100%";
      container.appendChild(canvas);

      const geometry = new Triangle(gl);

      const program = new Program(gl, {
        vertex,
        fragment,
        uniforms: {
          iTime: { value: 0 },
          iResolution: { value: new Float32Array([1, 1]) },
          uCustomColor: { value: new Float32Array(customColorRgb) },
          uUseCustomColor: { value: useCustomColor },
          uSpeed: { value: speed * 0.4 },
          uDirection: { value: directionMultiplier },
          uScale: { value: scale },
          uOpacity: { value: opacity },
          uMouse: { value: new Float32Array([0, 0]) },
          uMouseInteractive: { value: mouseInteractive ? 1.0 : 0.0 },
        },
      });

      const mesh = new Mesh(gl, { geometry, program });

      const onMouseMove = (e: MouseEvent) => {
        if (!mouseInteractive) return;
        const rect = container.getBoundingClientRect();
        mousePos.current.x = e.clientX - rect.left;
        mousePos.current.y = e.clientY - rect.top;
        const mouseUniform = program.uniforms.uMouse.value as Float32Array;
        mouseUniform[0] = mousePos.current.x;
        mouseUniform[1] = mousePos.current.y;
      };

      if (mouseInteractive) {
        container.addEventListener("mousemove", onMouseMove);
      }

      const setSize = () => {
        const rect = container.getBoundingClientRect();
        const width = Math.max(1, Math.floor(rect.width));
        const height = Math.max(1, Math.floor(rect.height));
        renderer?.setSize(width, height);
        const res = program.uniforms.iResolution.value as Float32Array;
        res[0] = gl.drawingBufferWidth;
        res[1] = gl.drawingBufferHeight;
      };

      ro = new ResizeObserver(setSize);
      ro.observe(container);
      setSize();

      const t0 = performance.now();
      const loop = (t: number) => {
        const timeValue = (t - t0) * 0.001;

        if (direction === "pingpong") {
          const cycle = Math.sin(timeValue * 0.5) * directionMultiplier;
          (program.uniforms.uDirection.value as number) = cycle;
        }

        (program.uniforms.iTime.value as number) = timeValue;
        renderer?.render({ scene: mesh });
        raf = requestAnimationFrame(loop);
      };
      raf = requestAnimationFrame(loop);

      return () => {
        cancelAnimationFrame(raf);
        ro?.disconnect();
        if (mouseInteractive) {
          container.removeEventListener("mousemove", onMouseMove);
        }
        if (canvas && container.contains(canvas)) {
          container.removeChild(canvas);
        }
        const loseContext = gl.getExtension("WEBGL_lose_context");
        loseContext?.loseContext();
      };
    } catch {
      setUseFallback(true);
      return () => {
        cancelAnimationFrame(raf);
        ro?.disconnect();
        if (canvas && container.contains(canvas)) {
          container.removeChild(canvas);
        }
      };
    }
  }, [color, speed, direction, scale, opacity, mouseInteractive, useFallback]);

  if (useFallback) {
    return (
      <div className="relative h-full w-full overflow-hidden">
        <div
          className="absolute inset-0 bg-gradient-to-br from-blue-50 via-indigo-50 to-slate-100"
          style={{ opacity }}
        />
        <div className="absolute -left-20 top-10 h-72 w-72 rounded-full bg-blue-300/30 blur-3xl" />
        <div className="absolute bottom-0 right-0 h-80 w-80 rounded-full bg-indigo-300/25 blur-3xl" />
        <div className="absolute left-1/3 top-1/2 h-64 w-64 rounded-full bg-sky-200/30 blur-3xl" />
      </div>
    );
  }

  return <div ref={containerRef} className="relative h-full w-full overflow-hidden" />;
};

function EzfinanzLogo() {
  return (
    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 shadow-md">
      <IndianRupee className="h-5 w-5 text-white" strokeWidth={2.5} />
    </div>
  );
}

function GlassmorphicNavbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const navLinks = [
    { label: "How it works", href: "#how-it-works" },
    { label: "Application", href: "#application" },
    { label: "Eligibility", href: "#eligibility" },
    { label: "Security", href: "#security" },
  ];

  return (
    <nav className="fixed top-4 left-1/2 z-50 w-11/12 w-full max-w-6xl -translate-x-1/2 transform px-4 sm:px-6 xl:px-0">
      <div className="rounded-full border border-white/20 bg-white/60 px-4 py-1 shadow-lg backdrop-blur-xl sm:px-6">
        <div className="flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2">
            <EzfinanzLogo />
            <span className="text-xl font-bold bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text text-transparent">
              EZFINANZ
            </span>
          </Link>

          <div className="hidden items-center space-x-1 md:flex">
            {navLinks.map((item) => (
              <a
                key={item.href}
                href={item.href}
                className="rounded-full px-4 py-2 font-medium text-gray-700 transition-colors hover:bg-white/50 hover:text-blue-700"
              >
                {item.label}
              </a>
            ))}
          </div>

          <div className="hidden items-center space-x-2 md:flex">
            <Link
              to="/login"
              className="rounded-full px-5 py-2 font-medium text-gray-700 transition-colors hover:bg-white/50"
            >
              Sign in
            </Link>
            <Link
              to="/signup"
              className="rounded-full bg-blue-600 px-5 py-2 font-medium text-white shadow-md transition-colors hover:bg-blue-700"
            >
              Apply now
            </Link>
          </div>

          <button
            type="button"
            className="rounded-full bg-white/50 p-2 md:hidden"
            onClick={() => setIsMenuOpen((open) => !open)}
            aria-label="Toggle menu"
          >
            <svg className="h-6 w-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {isMenuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {isMenuOpen && (
          <div className="mt-4 rounded-2xl border border-white/20 bg-white/90 p-4 shadow-lg md:hidden">
            <div className="flex flex-col space-y-3">
              {navLinks.map((item) => (
                <a
                  key={item.href}
                  href={item.href}
                  className="rounded-lg px-4 py-3 font-medium text-gray-700 transition-colors hover:bg-white hover:text-blue-700"
                  onClick={() => setIsMenuOpen(false)}
                >
                  {item.label}
                </a>
              ))}
              <div className="flex flex-col space-y-3 border-t border-gray-200 pt-3">
                <Link
                  to="/login"
                  className="rounded-lg bg-white/50 px-4 py-3 text-center font-medium text-gray-700"
                  onClick={() => setIsMenuOpen(false)}
                >
                  Sign in
                </Link>
                <Link
                  to="/signup"
                  className="rounded-lg bg-blue-600 px-4 py-3 text-center font-medium text-white shadow-md"
                  onClick={() => setIsMenuOpen(false)}
                >
                  Apply now
                </Link>
              </div>
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}

export interface HeroSectionProps {
  color?: string;
  speed?: number;
  scale?: number;
  opacity?: number;
  mouseInteractive?: boolean;
}

function HeroLottie() {
  return (
    <div className="relative w-full max-w-xl lg:max-w-none">
      <div className="pointer-events-none absolute inset-8 -z-10 rounded-full bg-blue-200/30 blur-3xl" />
      <Lottie
        src="/business-analysis.json"
        loop
        autoplay
        className="h-auto w-full"
        style={{ maxHeight: 480 }}
      />
    </div>
  );
}

export function HeroSection({
  color = "#2563eb",
  speed = 0.5,
  scale = 1.2,
  opacity = 0.15,
  mouseInteractive = true,
}: HeroSectionProps = {}) {
  return (
    <div className="relative min-h-screen w-full overflow-hidden">
      <GlassmorphicNavbar />

      <div className="absolute inset-0 z-0">
        <Plasma
          color={color}
          speed={speed}
          direction="forward"
          scale={scale}
          opacity={opacity}
          mouseInteractive={mouseInteractive}
        />
        <div className="absolute inset-0 bg-gradient-to-b from-white/40 to-white/80" />
      </div>

      <div className="relative z-10 mx-auto max-w-6xl px-4 pb-20 pt-36 sm:px-6 lg:px-8 lg:pt-44">
        <div className="grid items-center gap-10 lg:grid-cols-2 lg:gap-12">
          <div className="order-2 flex justify-center lg:order-1 lg:justify-start">
            <HeroLottie />
          </div>

          <div className="order-1 text-center lg:order-2 lg:text-left">
            <div className="mb-8 inline-flex items-center rounded-full bg-blue-100 px-4 py-2 text-sm font-medium text-blue-800">
              <Sparkles className="mr-2 h-4 w-4" />
              100% digital personal loans
            </div>

            <h1 className="pb-4 text-4xl font-extrabold tracking-tight text-gray-900 sm:text-5xl md:text-6xl">
              Your dreams.
              <span className="mt-2 block bg-gradient-to-r from-blue-700 to-indigo-600 bg-clip-text pb-2 text-transparent">
                Our commitment.
              </span>
            </h1>

            <p className="mx-auto mb-10 max-w-xl text-lg text-gray-600 sm:text-xl lg:mx-0">
              Apply for a personal loan in guided steps — email and phone verification, KYC, eligibility,
              EMI selection, bank details, declaration, and live selfie review.
            </p>

            <div className="mb-4 flex flex-col items-center gap-4 sm:flex-row lg:justify-start lg:items-stretch">
              <Link
                to="/signup"
                className="transform rounded-lg bg-blue-600 px-8 py-4 font-medium text-white shadow-lg transition-colors duration-300 hover:-translate-y-1 hover:bg-blue-700"
              >
                Start application
              </Link>
              <Link
                to="/login"
                className="flex items-center justify-center rounded-lg border border-gray-300 bg-white px-8 py-4 font-medium text-gray-900 shadow-sm transition-all duration-300 hover:shadow-md"
              >
                <PlayCircle className="mr-2 h-5 w-5" />
                Sign in to continue
              </Link>
            </div>
          </div>
        </div>

        <div id="how-it-works" className="mx-auto mt-16 mb-16 grid max-w-4xl grid-cols-2 gap-8 md:grid-cols-4">
          <div className="text-center">
            <div className="text-3xl font-bold text-gray-900">₹5L</div>
            <div className="text-gray-600">Loan up to</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-gray-900">8</div>
            <div className="text-gray-600">Guided steps</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-gray-900">100%</div>
            <div className="text-gray-600">Online process</div>
          </div>
          <div className="text-center">
            <div className="text-3xl font-bold text-gray-900">Live</div>
            <div className="text-gray-600">Admin review</div>
          </div>
        </div>

        <div id="application" className="mx-auto grid max-w-5xl gap-4 text-left sm:grid-cols-2 lg:grid-cols-4">
          {[
            "Verify email & phone",
            "Complete KYC",
            "Check eligibility & EMI",
            "Selfie verification",
          ].map((step) => (
            <div key={step} className="rounded-2xl border border-white/60 bg-white/70 p-4 shadow-sm backdrop-blur-sm">
              <p className="text-sm font-semibold text-blue-700">Step</p>
              <p className="mt-1 text-sm text-gray-700">{step}</p>
            </div>
          ))}
        </div>

        <div id="eligibility" className="mx-auto mt-10 max-w-3xl rounded-2xl border border-blue-100 bg-white/80 p-6 shadow-sm backdrop-blur-sm">
          <p className="text-sm font-semibold uppercase tracking-wide text-blue-700">Eligibility snapshot</p>
          <p className="mt-2 text-gray-600">
            Salaried and self-employed applicants can check eligibility instantly. Approved applications move to
            bank capture, declaration, and disbursement after admin selfie review.
          </p>
        </div>

        <div id="security" className="mx-auto mt-10 flex max-w-3xl flex-col items-center justify-center gap-3 text-sm text-gray-600 sm:flex-row sm:gap-8">
          <p className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-blue-600" />
            Bank-level security
          </p>
          <p className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-blue-600" />
            Encrypted document storage
          </p>
          <p className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-blue-600" />
            Role-based admin access
          </p>
        </div>
      </div>

      <div className="absolute left-10 top-1/4 h-6 w-6 rounded-full bg-blue-200/40 blur-xl" />
      <div className="absolute bottom-1/3 right-16 h-10 w-10 rounded-full bg-indigo-200/30 blur-xl" />
      <div className="absolute right-1/4 top-1/3 h-8 w-8 rounded-full bg-blue-200/40 blur-xl" />
    </div>
  );
}

export default HeroSection;
