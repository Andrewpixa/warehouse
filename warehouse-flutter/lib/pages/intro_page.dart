import 'dart:async';
import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../api/auth_api.dart';
import 'home_page.dart';
import 'login_page.dart';

const _ink = Color(0xFFF6F1E8);
const _muted = Color(0xA3F6F1E8);
const _faint = Color(0x61F6F1E8);
const _line = Color(0x1AF6F1E8);
const _rose = Color(0xFFFB7185);
const _roseDeep = Color(0xFFE11D48);
const _gold = Color(0xFFE8C39A);
const _bg = Color(0xFF07070A);

class IntroPage extends StatefulWidget {
  const IntroPage({super.key, this.user, this.playBoot = true});

  final AuthUser? user;
  final bool playBoot;

  @override
  State<IntroPage> createState() => _IntroPageState();
}

class _IntroPageState extends State<IntroPage> with TickerProviderStateMixin {
  late final AnimationController _boot;
  late final AnimationController _loop;
  late final AnimationController _reveal;
  final _scroll = ScrollController();
  final _capKey = GlobalKey();
  double _progress = 0;
  bool _scrolled = false;
  bool _booting = true;

  @override
  void initState() {
    super.initState();
    _boot = AnimationController(vsync: this, duration: const Duration(milliseconds: 1100));
    _loop = AnimationController(vsync: this, duration: const Duration(seconds: 8))..repeat();
    _reveal = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400));
    _scroll.addListener(_onScroll);

    final reduce = SchedulerBinding.instance.platformDispatcher.accessibilityFeatures.disableAnimations;
    if (!widget.playBoot || reduce) {
      _booting = false;
      _reveal.value = 1;
    } else {
      _boot.forward().whenComplete(() {
        if (!mounted) return;
        setState(() => _booting = false);
        _reveal.forward();
      });
    }
  }

  void _onScroll() {
    if (!_scroll.hasClients) return;
    final max = math.max(1.0, _scroll.position.maxScrollExtent);
    setState(() {
      _progress = (_scroll.offset / max).clamp(0.0, 1.0);
      _scrolled = _scroll.offset > 20;
    });
  }

  @override
  void dispose() {
    _boot.dispose();
    _loop.dispose();
    _reveal.dispose();
    _scroll.dispose();
    super.dispose();
  }

  void _scrollToCaps() {
    final ctx = _capKey.currentContext;
    if (ctx != null) {
      Scrollable.ensureVisible(ctx, duration: const Duration(milliseconds: 520), curve: Curves.easeOutCubic);
    }
  }

  void _enter() {
    if (Navigator.of(context).canPop() && widget.user != null) {
      Navigator.of(context).pop();
      return;
    }
    final logged = widget.user != null;
    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        pageBuilder: (_, __, ___) =>
            logged ? HomePage(user: widget.user!) : const LoginPage(),
        transitionsBuilder: (_, a, __, child) =>
            FadeTransition(opacity: a, child: child),
        transitionDuration: const Duration(milliseconds: 380),
      ),
    );
  }

  Animation<double> _stagger(double start, double end) {
    return CurvedAnimation(
      parent: _reveal,
      curve: Interval(start, end, curve: Curves.easeOutCubic),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cta = widget.user != null ? '进入工作台' : '进入系统';
    return Scaffold(
      backgroundColor: _bg,
      body: Stack(
        children: [
          NotificationListener<ScrollNotification>(
            onNotification: (_) => false,
            child: CustomScrollView(
              controller: _scroll,
              slivers: [
                SliverToBoxAdapter(child: _HeroBlock(
                  loop: _loop,
                  titleA: _stagger(0.05, 0.42),
                  titleB: _stagger(0.12, 0.5),
                  sub: _stagger(0.2, 0.58),
                  actions: _stagger(0.28, 0.66),
                  meta: _stagger(0.36, 0.74),
                  stage: _stagger(0.18, 0.62),
                  cta: cta,
                  onEnter: _enter,
                  onLearn: _scrollToCaps,
                )),
                SliverToBoxAdapter(child: _Marquee(loop: _loop)),
                SliverToBoxAdapter(
                  child: _ViewportFade(
                    listenable: _scroll,
                    child: _Capabilities(key: _capKey),
                  ),
                ),
                SliverToBoxAdapter(
                  child: _ViewportFade(listenable: _scroll, child: const _TraceFlow()),
                ),
                SliverToBoxAdapter(
                  child: _ViewportFade(listenable: _scroll, child: const _Trust()),
                ),
                SliverToBoxAdapter(
                  child: _ViewportFade(
                    listenable: _scroll,
                    child: _CtaBand(cta: cta, onEnter: _enter),
                  ),
                ),
                const SliverToBoxAdapter(child: _Foot()),
              ],
            ),
          ),
          _TopProgress(progress: _progress),
          _StickyNav(
            scrolled: _scrolled,
            cta: cta,
            onEnter: _enter,
          ),
          if (_booting) _BootOverlay(controller: _boot),
        ],
      ),
    );
  }
}

class _TopProgress extends StatelessWidget {
  const _TopProgress({required this.progress});
  final double progress;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: IgnorePointer(
        child: SizedBox(
          height: 2,
          child: Align(
            alignment: Alignment.centerLeft,
            child: FractionallySizedBox(
              widthFactor: progress,
              child: const DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: [_roseDeep, _gold]),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _BootOverlay extends StatelessWidget {
  const _BootOverlay({required this.controller});
  final AnimationController controller;

  @override
  Widget build(BuildContext context) {
    final fade = CurvedAnimation(
      parent: controller,
      curve: const Interval(0.72, 1, curve: Curves.easeOut),
    );
    final load = CurvedAnimation(
      parent: controller,
      curve: const Interval(0, 0.82, curve: Curves.easeInOut),
    );
    return FadeTransition(
      opacity: Tween(begin: 1.0, end: 0.0).animate(fade),
      child: IgnorePointer(
        child: Container(
          color: const Color(0xFF050506),
          alignment: Alignment.center,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              ScaleTransition(
                scale: CurvedAnimation(parent: controller, curve: Curves.easeOutBack),
                child: Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    gradient: const LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [_roseDeep, _rose],
                    ),
                  ),
                  child: const Icon(Icons.hexagon_outlined, color: Colors.white, size: 22),
                ),
              ),
              const SizedBox(height: 18),
              const Text(
                'PHARMA IMS',
                style: TextStyle(
                  color: _faint,
                  fontSize: 11,
                  letterSpacing: 8.4,
                ),
              ),
              const SizedBox(height: 18),
              SizedBox(
                width: 120,
                height: 2,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(2),
                  child: AnimatedBuilder(
                    animation: load,
                    builder: (_, __) => Align(
                      alignment: Alignment.centerLeft,
                      child: FractionallySizedBox(
                        widthFactor: 0.18 + load.value * 0.82,
                        child: const DecoratedBox(
                          decoration: BoxDecoration(
                            gradient: LinearGradient(colors: [_roseDeep, _gold]),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StickyNav extends StatelessWidget {
  const _StickyNav({
    required this.scrolled,
    required this.cta,
    required this.onEnter,
  });

  final bool scrolled;
  final String cta;
  final VoidCallback onEnter;

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: ClipRect(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: scrolled ? 18 : 0, sigmaY: scrolled ? 18 : 0),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 280),
            height: 64 + MediaQuery.paddingOf(context).top,
            padding: EdgeInsets.only(
              top: MediaQuery.paddingOf(context).top,
              left: 20,
              right: 16,
            ),
            decoration: BoxDecoration(
              color: scrolled ? const Color(0xB807070A) : Colors.transparent,
              border: Border(
                bottom: BorderSide(color: scrolled ? _line : Colors.transparent),
              ),
            ),
            child: Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(9),
                    gradient: const LinearGradient(
                      colors: [_roseDeep, _rose],
                    ),
                  ),
                  child: const Icon(Icons.hexagon_outlined, color: Colors.white, size: 16),
                ),
                const SizedBox(width: 10),
                const Text(
                  '药企进销存',
                  style: TextStyle(
                    color: _ink,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.2,
                    fontSize: 13,
                  ),
                ),
                const Spacer(),
                FilledButton(
                  onPressed: onEnter,
                  style: FilledButton.styleFrom(
                    backgroundColor: _roseDeep,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    minimumSize: const Size(0, 36),
                    shape: const StadiumBorder(),
                    elevation: 0,
                  ),
                  child: Text(cta, style: const TextStyle(fontSize: 12, letterSpacing: 1.4)),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _HeroBlock extends StatelessWidget {
  const _HeroBlock({
    required this.loop,
    required this.titleA,
    required this.titleB,
    required this.sub,
    required this.actions,
    required this.meta,
    required this.stage,
    required this.cta,
    required this.onEnter,
    required this.onLearn,
  });

  final AnimationController loop;
  final Animation<double> titleA;
  final Animation<double> titleB;
  final Animation<double> sub;
  final Animation<double> actions;
  final Animation<double> meta;
  final Animation<double> stage;
  final String cta;
  final VoidCallback onEnter;
  final VoidCallback onLearn;

  @override
  Widget build(BuildContext context) {
    final top = MediaQuery.paddingOf(context).top + 80;
    return SizedBox(
      height: math.max(780, MediaQuery.sizeOf(context).height * 0.92),
      child: Stack(
        children: [
          Positioned.fill(child: _HeroBg(loop: loop)),
          Padding(
            padding: EdgeInsets.fromLTRB(24, top, 24, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Reveal(
                  animation: titleA,
                  child: const Row(
                    children: [
                      _PulseDot(),
                      SizedBox(width: 8),
                      Text(
                        'GSP · 批号库存 · 追溯码',
                        style: TextStyle(color: _gold, fontSize: 12, letterSpacing: 2),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                _Reveal(
                  animation: titleA,
                  child: const Text(
                    '让每一粒药',
                    style: TextStyle(
                      color: _ink,
                      fontSize: 36,
                      fontWeight: FontWeight.w700,
                      height: 1.15,
                    ),
                  ),
                ),
                _Reveal(
                  animation: titleB,
                  child: ShaderMask(
                    shaderCallback: (r) => const LinearGradient(
                      colors: [_rose, _gold],
                    ).createShader(r),
                    child: const Text(
                      '都有据可循',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 36,
                        fontWeight: FontWeight.w700,
                        height: 1.15,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                _Reveal(
                  animation: sub,
                  child: const Text(
                    '为医药流通企业打造的进销存系统。采购入库、批号库存、销售出库、医院收货与日清月结，一条链路贯穿药品流通。',
                    style: TextStyle(color: _muted, fontSize: 14, height: 1.6),
                  ),
                ),
                const SizedBox(height: 22),
                _Reveal(
                  animation: actions,
                  child: Wrap(
                    spacing: 12,
                    runSpacing: 10,
                    children: [
                      FilledButton(
                        onPressed: onEnter,
                        style: FilledButton.styleFrom(
                          backgroundColor: _roseDeep,
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 14),
                          shape: const StadiumBorder(),
                        ),
                        child: Text(cta),
                      ),
                      OutlinedButton(
                        onPressed: onLearn,
                        style: OutlinedButton.styleFrom(
                          foregroundColor: _ink,
                          side: const BorderSide(color: _line),
                          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
                          shape: const StadiumBorder(),
                        ),
                        child: const Text('了解产品能力  ↓'),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                _Reveal(
                  animation: meta,
                  child: const Wrap(
                    spacing: 16,
                    children: [
                      _Meta('批号级精度'),
                      _Meta('全链路追溯'),
                      _Meta('日清 / 月结闭环'),
                    ],
                  ),
                ),
                const SizedBox(height: 28),
                Expanded(
                  child: _Reveal(
                    animation: stage,
                    child: _DeviceStage(loop: loop),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PulseDot extends StatefulWidget {
  const _PulseDot();

  @override
  State<_PulseDot> createState() => _PulseDotState();
}

class _PulseDotState extends State<_PulseDot> with SingleTickerProviderStateMixin {
  late final AnimationController c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..repeat();

  @override
  void dispose() {
    c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: c,
      builder: (_, __) {
        final s = 0.7 + 0.3 * math.sin(c.value * math.pi * 2);
        return Container(
          width: 8 * s,
          height: 8 * s,
          decoration: BoxDecoration(
            color: _rose,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(color: _rose.withValues(alpha: 0.55), blurRadius: 8),
            ],
          ),
        );
      },
    );
  }
}

class _Meta extends StatelessWidget {
  const _Meta(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(width: 4, height: 4, decoration: const BoxDecoration(color: _gold, shape: BoxShape.circle)),
        const SizedBox(width: 6),
        Text(text, style: const TextStyle(color: _faint, fontSize: 12)),
      ],
    );
  }
}

class _HeroBg extends StatelessWidget {
  const _HeroBg({required this.loop});
  final AnimationController loop;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: loop,
      builder: (_, __) {
        final t = loop.value * math.pi * 2;
        return CustomPaint(
          painter: _HeroBgPainter(t),
          child: const SizedBox.expand(),
        );
      },
    );
  }
}

class _HeroBgPainter extends CustomPainter {
  _HeroBgPainter(this.t);
  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    final grid = Paint()
      ..color = const Color(0x09FFFFFF)
      ..strokeWidth = 1;
    const step = 72.0;
    for (double x = 0; x < size.width; x += step) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), grid);
    }
    for (double y = 0; y < size.height; y += step) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), grid);
    }

    void orb(Offset c, double r, Color color) {
      final p = Paint()
        ..shader = RadialGradient(
          colors: [color.withValues(alpha: 0.28), color.withValues(alpha: 0)],
        ).createShader(Rect.fromCircle(center: c, radius: r));
      canvas.drawCircle(c, r, p);
    }

    orb(Offset(size.width * 0.18 + 18 * math.sin(t), size.height * 0.22), 160, _roseDeep);
    orb(Offset(size.width * 0.82 + 22 * math.cos(t), size.height * 0.38), 200, _gold);
    orb(Offset(size.width * 0.5, size.height * 0.78 + 16 * math.sin(t * 0.7)), 140, _rose);

    final speck = Paint()..color = const Color(0x33F6F1E8);
    for (var n = 1; n <= 18; n++) {
      final left = ((n * 37) % 100) / 100 * size.width;
      final top = ((n * 53) % 90) / 100 * size.height;
      final bob = math.sin(t + n) * 8;
      canvas.drawCircle(Offset(left, top + bob), 1.4, speck);
    }
  }

  @override
  bool shouldRepaint(covariant _HeroBgPainter old) => old.t != t;
}

class _DeviceStage extends StatelessWidget {
  const _DeviceStage({required this.loop});
  final AnimationController loop;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: loop,
      builder: (_, child) {
        final t = loop.value * math.pi * 2;
        return Transform(
          alignment: Alignment.center,
          transform: Matrix4.identity()
            ..setEntry(3, 2, 0.0012)
            ..rotateX(0.07 + 0.02 * math.sin(t))
            ..rotateY(-0.14 + 0.03 * math.cos(t)),
          child: child,
        );
      },
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: _line),
              color: const Color(0x0DFFFFFF),
              boxShadow: [
                BoxShadow(
                  color: _roseDeep.withValues(alpha: 0.18),
                  blurRadius: 40,
                  spreadRadius: 4,
                ),
              ],
            ),
            child: Column(
              children: [
                Container(
                  height: 36,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  decoration: const BoxDecoration(
                    border: Border(bottom: BorderSide(color: _line)),
                  ),
                  child: const Row(
                    children: [
                      _Dot(Color(0xFFF87171)),
                      SizedBox(width: 4),
                      _Dot(Color(0xFFFBBF24)),
                      SizedBox(width: 4),
                      _Dot(Color(0xFF34D399)),
                      SizedBox(width: 10),
                      Text('批号库存 · LIVE', style: TextStyle(color: _muted, fontSize: 11)),
                      Spacer(),
                      Text('TRACE ON', style: TextStyle(color: _gold, fontSize: 10, letterSpacing: 1.2)),
                    ],
                  ),
                ),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      children: [
                        const Row(
                          children: [
                            _MiniStat('1,284', '在库批号'),
                            _MiniStat('03', '近效期'),
                            _MiniStat('46', '今日出库'),
                          ],
                        ),
                        const SizedBox(height: 12),
                        _ScanBox(loop: loop),
                        const SizedBox(height: 10),
                        const _BatchRow('阿莫西林胶囊 0.25g', '240918A', '近效 86 天', Color(0xFFFBBF24)),
                        const _BatchRow('头孢克肟分散片', '240722B', '库存充足', Color(0xFF34D399)),
                        const _BatchRow('注射用奥美拉唑', '241103C', '已关联码', Color(0xFF60A5FA)),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          const Positioned(
            right: -8,
            top: -12,
            child: _FloatCard('近效期预警', '3 个批号不足 90 天'),
          ),
          const Positioned(
            left: -6,
            bottom: -10,
            child: _FloatCard('医院收货', '今日已确认 12 单'),
          ),
        ],
      ),
    );
  }
}

class _Dot extends StatelessWidget {
  const _Dot(this.color);
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(width: 7, height: 7, decoration: BoxDecoration(color: color, shape: BoxShape.circle));
  }
}

class _MiniStat extends StatelessWidget {
  const _MiniStat(this.v, this.k);
  final String v;
  final String k;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Text(v, style: const TextStyle(color: _ink, fontSize: 16, fontWeight: FontWeight.w700)),
          Text(k, style: const TextStyle(color: _faint, fontSize: 10)),
        ],
      ),
    );
  }
}

class _ScanBox extends StatelessWidget {
  const _ScanBox({required this.loop});
  final AnimationController loop;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 10),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(10),
        color: const Color(0x14000000),
        border: Border.all(color: _line),
      ),
      child: Row(
        children: [
          Expanded(
            child: AnimatedBuilder(
              animation: loop,
              builder: (_, __) {
                return CustomPaint(
                  painter: _BarcodePainter(loop.value),
                  child: const SizedBox(height: 28),
                );
              },
            ),
          ),
          const SizedBox(width: 10),
          const Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('(01)06901234567890', style: TextStyle(color: _ink, fontSize: 10, letterSpacing: 0.4)),
              Text('追溯码已关联 · 爱创解析通过', style: TextStyle(color: Color(0xFF34D399), fontSize: 9)),
            ],
          ),
        ],
      ),
    );
  }
}

class _BarcodePainter extends CustomPainter {
  _BarcodePainter(this.t);
  final double t;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = _ink.withValues(alpha: 0.75);
    for (var i = 0; i < 28; i++) {
      final w = 1.0 + (i * 17 % 5);
      final o = 0.35 + (i * 13 % 7) / 10;
      paint.color = _ink.withValues(alpha: o);
      canvas.drawRect(Rect.fromLTWH(i * 3.2, 2, w, size.height - 4), paint);
    }
    final x = (t % 1) * size.width;
    canvas.drawRect(
      Rect.fromLTWH(x, 0, 2, size.height),
      Paint()
        ..color = _rose
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 2),
    );
  }

  @override
  bool shouldRepaint(covariant _BarcodePainter old) => old.t != t;
}

class _BatchRow extends StatelessWidget {
  const _BatchRow(this.name, this.lot, this.tag, this.tone);
  final String name;
  final String lot;
  final String tag;
  final Color tone;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(child: Text(name, style: const TextStyle(color: _ink, fontSize: 11), overflow: TextOverflow.ellipsis)),
          Text(lot, style: const TextStyle(color: _faint, fontSize: 10)),
          const SizedBox(width: 8),
          Text(tag, style: TextStyle(color: tone, fontSize: 10)),
        ],
      ),
    );
  }
}

class _FloatCard extends StatelessWidget {
  const _FloatCard(this.title, this.sub);
  final String title;
  final String sub;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: const Color(0x9907070A),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: _line),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(color: _ink, fontSize: 11, fontWeight: FontWeight.w700)),
              Text(sub, style: const TextStyle(color: _muted, fontSize: 10)),
            ],
          ),
        ),
      ),
    );
  }
}

class _Marquee extends StatefulWidget {
  const _Marquee({required this.loop});
  final AnimationController loop;

  static const items = [
    '采购入库', '批号库存', '追溯码查询', '销售出库', '医院收货',
    '日清检查', '月结对账', '多仓调拨', '利润分析', '盘点管理',
  ];

  @override
  State<_Marquee> createState() => _MarqueeState();
}

class _MarqueeState extends State<_Marquee> {
  final _trackKey = GlobalKey();
  double _trackW = 1;

  List<Widget> _chips() {
    return [
      for (final s in _Marquee.items)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          child: Text(
            s,
            style: const TextStyle(color: _faint, letterSpacing: 2, fontSize: 12),
          ),
        ),
    ];
  }

  @override
  Widget build(BuildContext context) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final box = _trackKey.currentContext?.findRenderObject() as RenderBox?;
      if (box != null && box.hasSize && (box.size.width - _trackW).abs() > 0.5) {
        setState(() => _trackW = box.size.width);
      }
    });
    return SizedBox(
      height: 48,
      width: double.infinity,
      child: ShaderMask(
        blendMode: BlendMode.dstIn,
        shaderCallback: (rect) {
          return const LinearGradient(
            colors: [
              Color(0x00000000),
              Color(0xFFFFFFFF),
              Color(0xFFFFFFFF),
              Color(0x00000000),
            ],
            stops: [0.0, 0.12, 0.88, 1.0],
          ).createShader(rect);
        },
        child: ClipRect(
          child: OverflowBox(
            maxWidth: double.infinity,
            alignment: Alignment.centerLeft,
            child: AnimatedBuilder(
              animation: widget.loop,
              builder: (_, child) {
                final shift = widget.loop.value * _trackW;
                return Transform.translate(
                  offset: Offset(-shift, 0),
                  child: child,
                );
              },
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  KeyedSubtree(
                    key: _trackKey,
                    child: Row(mainAxisSize: MainAxisSize.min, children: _chips()),
                  ),
                  Row(mainAxisSize: MainAxisSize.min, children: _chips()),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ViewportFade extends StatelessWidget {
  const _ViewportFade({required this.listenable, required this.child});

  final Listenable listenable;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: listenable,
      builder: (context, child) {
        var opacity = 1.0;
        final box = context.findRenderObject() as RenderBox?;
        if (box != null && box.hasSize && box.attached) {
          final y = box.localToGlobal(Offset.zero).dy;
          final h = box.size.height;
          final vh = MediaQuery.sizeOf(context).height;
          final topFade = 88 + MediaQuery.paddingOf(context).top;
          if (y > vh - 100) {
            opacity = ((vh - y) / 100).clamp(0.0, 1.0);
          }
          if (y < topFade) {
            final visible = (y + h - topFade) / math.min(h, 180);
            opacity = math.min(opacity, visible.clamp(0.0, 1.0));
          }
        }
        final t = Curves.easeInOut.transform(opacity.clamp(0.0, 1.0));
        return Opacity(
          opacity: t,
          child: Transform.translate(
            offset: Offset(0, (1 - t) * 18),
            child: child,
          ),
        );
      },
      child: child,
    );
  }
}

class _Capabilities extends StatelessWidget {
  const _Capabilities({super.key});

  static const _cards = <_CapData>[
    _CapData(
      asset: 'assets/intro/cap-lot.png',
      kicker: '01 · 批号库存',
      title: '每一批货，效期与数量都在掌握中',
      body: '按批号管理库存，近效期自动预警。升益、盘点、分仓不再只看品种合计。',
      tag: 'LOT · EXPIRY',
      featured: true,
    ),
    _CapData(
      asset: 'assets/intro/cap-trace.png',
      kicker: '02 · 追溯码',
      title: 'GS1 / 01 码分流查询',
      body: '扫码即见来源与流向，关联入库、出库与医院收货，对接码上放心。',
      tag: 'GS1 · SCAN',
    ),
    _CapData(
      asset: 'assets/intro/cap-io.png',
      kicker: '03 · 入出库闭环',
      title: '采购进、销售出，单据不断档',
      body: '进货订单、出库单、退加货记录串成一条作业流。',
      tag: 'IN · OUT',
    ),
    _CapData(
      asset: 'assets/intro/cap-hospital.png',
      kicker: '04 · 医院收货',
      title: '下游确认，流向落袋为安',
      body: '医院收货确认回写单据，减少对账扯皮与流向争议。',
      tag: 'HOSPITAL',
    ),
    _CapData(
      asset: 'assets/intro/cap-settle.png',
      kicker: '05 · 日清月结',
      title: '每天收口，每月对账',
      body: '日清检查与月结对账把库存、金额、追溯一次对齐。',
      tag: 'DAY · MONTH',
    ),
    _CapData(
      asset: 'assets/intro/cap-warehouse.png',
      kicker: '06 · 多仓协同',
      title: '调拨、分仓、盘点一体',
      body: '仓库之间调拨可追踪，分仓库存独立核算。',
      tag: 'MULTI-WH',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('PRODUCT CAPABILITIES', style: TextStyle(color: _gold, fontSize: 11, letterSpacing: 2.4)),
          const SizedBox(height: 8),
          const Text('一套系统，覆盖流通全链路', style: TextStyle(color: _ink, fontSize: 24, fontWeight: FontWeight.w700, height: 1.25)),
          const SizedBox(height: 8),
          const Text(
            '从入库到出库，从批号到追溯码，把日常作业做成可审计、可回溯的闭环。',
            style: TextStyle(color: _muted, height: 1.5),
          ),
          const SizedBox(height: 22),
          LayoutBuilder(
            builder: (context, cons) {
              final two = cons.maxWidth >= 720;
              if (!two) {
                return Column(children: [for (final c in _cards) _CapCard(data: c)]);
              }
              return Column(
                children: [
                  _CapCard(data: _cards.first),
                  for (var i = 1; i < _cards.length; i += 2)
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(child: _CapCard(data: _cards[i])),
                        const SizedBox(width: 16),
                        Expanded(
                          child: i + 1 < _cards.length
                              ? _CapCard(data: _cards[i + 1])
                              : const SizedBox.shrink(),
                        ),
                      ],
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}

class _CapData {
  const _CapData({
    required this.asset,
    required this.kicker,
    required this.title,
    required this.body,
    required this.tag,
    this.featured = false,
  });

  final String asset;
  final String kicker;
  final String title;
  final String body;
  final String tag;
  final bool featured;
}

class _CapCard extends StatelessWidget {
  const _CapCard({required this.data});
  final _CapData data;

  @override
  Widget build(BuildContext context) {
    final h = data.featured ? 268.0 : 168.0;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _line),
        boxShadow: [
          BoxShadow(
            color: _roseDeep.withValues(alpha: data.featured ? 0.16 : 0.08),
            blurRadius: 28,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            height: h,
            width: double.infinity,
            child: Stack(
              fit: StackFit.expand,
              children: [
                Image.asset(data.asset, fit: BoxFit.cover),
                const DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        Color(0x0007070A),
                        Color(0x6607070A),
                        Color(0xF207070A),
                      ],
                      stops: [0.15, 0.52, 1],
                    ),
                  ),
                ),
                Positioned(
                  top: 14,
                  left: 14,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: const Color(0x9907070A),
                      borderRadius: BorderRadius.circular(999),
                      border: Border.all(color: _gold.withValues(alpha: 0.35)),
                    ),
                    child: Text(
                      data.tag,
                      style: const TextStyle(color: _gold, fontSize: 9, letterSpacing: 1.6, fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
                Positioned(
                  left: 16,
                  right: 16,
                  bottom: 16,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(data.kicker, style: const TextStyle(color: _gold, fontSize: 11, letterSpacing: 1.3)),
                      const SizedBox(height: 6),
                      Text(
                        data.title,
                        style: TextStyle(
                          color: _ink,
                          fontSize: data.featured ? 20 : 16,
                          fontWeight: FontWeight.w700,
                          height: 1.25,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Container(
            width: double.infinity,
            color: const Color(0xFF0C0C10),
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
            child: Text(data.body, style: const TextStyle(color: _muted, fontSize: 13, height: 1.55)),
          ),
        ],
      ),
    );
  }
}

class _Slide {
  const _Slide({
    required this.assets,
    required this.kicker,
    required this.title,
    required this.body,
  });

  final List<String> assets;
  final String kicker;
  final String title;
  final String body;
}

class _TraceFlow extends StatefulWidget {
  const _TraceFlow();

  @override
  State<_TraceFlow> createState() => _TraceFlowState();
}

class _TraceFlowState extends State<_TraceFlow> {
  static const _steps = [
    _Slide(
      assets: ['assets/intro/cap-io.png', 'assets/intro/cap-warehouse.png'],
      kicker: '01',
      title: '采购入库',
      body: '供应商到货，批号入账',
    ),
    _Slide(
      assets: ['assets/intro/cap-lot.png'],
      kicker: '02',
      title: '在库管理',
      body: '效期、升益、盘点',
    ),
    _Slide(
      assets: ['assets/intro/cap-trace.png', 'assets/intro/cap-io.png'],
      kicker: '03',
      title: '销售出库',
      body: '开单出库，码随货走',
    ),
    _Slide(
      assets: ['assets/intro/cap-hospital.png'],
      kicker: '04',
      title: '医院收货',
      body: '下游确认流向',
    ),
    _Slide(
      assets: ['assets/intro/cap-trace.png'],
      kicker: '05',
      title: '码上放心',
      body: '追溯链路闭环',
    ),
  ];

  int _index = 0;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('TRACEABILITY', style: TextStyle(color: _gold, fontSize: 11, letterSpacing: 2.4)),
          const SizedBox(height: 8),
          const Text('药品流向，像时间线一样可读', style: TextStyle(color: _ink, fontSize: 24, fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          const Text('从供应商入库到医院收货，每一步都留下可核验的痕迹。', style: TextStyle(color: _muted, height: 1.5)),
          const SizedBox(height: 18),
          _DepthTunnel(
            slides: _steps,
            height: 300,
            selected: _index,
            onChanged: (i) => setState(() => _index = i),
          ),
          const SizedBox(height: 18),
          for (var i = 0; i < _steps.length; i++)
            GestureDetector(
              onTap: () => setState(() => _index = i),
              behavior: HitTestBehavior.opaque,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Column(
                    children: [
                      Container(
                        width: 10,
                        height: 10,
                        decoration: BoxDecoration(
                          color: i == _index ? _rose : _faint,
                          shape: BoxShape.circle,
                        ),
                      ),
                      if (i < _steps.length - 1)
                        Container(width: 1, height: 44, color: _line),
                    ],
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(bottom: 18),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${_steps[i].kicker}  ${_steps[i].title}',
                            style: TextStyle(
                              color: i == _index ? _ink : _muted,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Text(_steps[i].body, style: const TextStyle(color: _muted, fontSize: 13)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _Trust extends StatelessWidget {
  const _Trust();

  static const _items = [
    _Slide(
      assets: ['assets/intro/cap-lot.png', 'assets/intro/cap-warehouse.png'],
      kicker: '01',
      title: '批号库存可核对',
      body: '库存落到批号：数量、效期、仓位都能逐批核对，不再只看到品种合计。',
    ),
    _Slide(
      assets: ['assets/intro/cap-trace.png'],
      kicker: '02',
      title: 'GS1 / 01 码可查来源去向',
      body: '扫码即可看到入库来源与出库流向，对接码上放心的业务节奏。',
    ),
    _Slide(
      assets: ['assets/intro/cap-io.png', 'assets/intro/cap-hospital.png', 'assets/intro/cap-settle.png'],
      kicker: '03',
      title: '入出库、收货、日清月结留痕',
      body: '采购入库、销售出库、医院收货、日清月结都留下操作记录，对账与检查有据可依。',
    ),
    _Slide(
      assets: ['assets/intro/cap-settle.png', 'assets/intro/cap-lot.png'],
      kicker: '04',
      title: '近效期预警、红冲可追溯',
      body: '近效期批号提前告警；红冲单关联原发票，冲减数量与库存回补可完整回溯。',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.fromLTRB(24, 8, 24, 16),
      child: _FlipDeck(slides: _items, height: 400),
    );
  }
}

class _DepthTunnel extends StatefulWidget {
  const _DepthTunnel({
    required this.slides,
    required this.height,
    this.selected,
    this.onChanged,
  });

  final List<_Slide> slides;
  final double height;
  final int? selected;
  final ValueChanged<int>? onChanged;

  @override
  State<_DepthTunnel> createState() => _DepthTunnelState();
}

class _DepthTunnelState extends State<_DepthTunnel> {
  late final int _n = widget.slides.length;
  late final PageController _pages = PageController(viewportFraction: 0.8, initialPage: _n * 40);
  Timer? _auto;
  int _index = 0;

  int _real(int page) => ((page % _n) + _n) % _n;

  @override
  void initState() {
    super.initState();
    _auto = Timer.periodic(const Duration(seconds: 5), (_) {
      if (!mounted || !_pages.hasClients || _pages.position.isScrollingNotifier.value) return;
      final current = _pages.page?.round() ?? _n * 40;
      _pages.animateToPage(current + 1, duration: const Duration(milliseconds: 650), curve: Curves.easeOutCubic);
    });
  }

  @override
  void didUpdateWidget(covariant _DepthTunnel oldWidget) {
    super.didUpdateWidget(oldWidget);
    final target = widget.selected;
    if (target == null || target == _index || !_pages.hasClients) return;
    if (_pages.position.isScrollingNotifier.value) return;
    _go(target);
  }

  @override
  void dispose() {
    _auto?.cancel();
    _pages.dispose();
    super.dispose();
  }

  void _go(int target) {
    final current = _pages.page?.round() ?? _n * 40;
    var delta = target - _real(current);
    if (delta > _n / 2) delta -= _n;
    if (delta < -_n / 2) delta += _n;
    _pages.animateToPage(current + delta, duration: const Duration(milliseconds: 650), curve: Curves.easeOutCubic);
  }

  @override
  Widget build(BuildContext context) {
    final slide = widget.slides[_index];
    return Column(
      children: [
        SizedBox(
          height: widget.height,
          child: PageView.builder(
            controller: _pages,
            itemBuilder: (context, i) {
              final real = _real(i);
              return AnimatedBuilder(
                animation: _pages,
                builder: (context, child) {
                  final page = _pages.hasClients ? (_pages.page ?? i.toDouble()) : i.toDouble();
                  final delta = (i - page).clamp(-1.0, 1.0);
                  final focus = (1 - delta.abs()).clamp(0.0, 1.0);
                  return Transform(
                    alignment: Alignment.center,
                    transform: Matrix4.identity()
                      ..setEntry(3, 2, 0.0016)
                      ..rotateY(-delta * 0.55)
                      ..scaleByDouble(0.88 + 0.12 * focus, 0.88 + 0.12 * focus, 1, 1),
                    child: Opacity(opacity: 0.62 + 0.38 * focus, child: child),
                  );
                },
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(10, 16, 10, 20),
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(18),
                      boxShadow: [
                        BoxShadow(
                          color: _roseDeep.withValues(alpha: 0.28),
                          blurRadius: 24,
                          offset: const Offset(0, 12),
                        ),
                      ],
                    ),
                    child: _SlideFrame(slide: widget.slides[real], showCaption: false),
                  ),
                ),
              );
            },
            onPageChanged: (i) {
              final real = _real(i);
              if (real == _index) return;
              setState(() => _index = real);
              widget.onChanged?.call(real);
            },
          ),
        ),
        _Dots(
          count: _n,
          index: _index,
          label: '${slide.kicker}  ${slide.title}',
          onTap: (page) => _go(page.round()),
        ),
      ],
    );
  }
}

class _FlipDeck extends StatefulWidget {
  const _FlipDeck({required this.slides, required this.height});

  final List<_Slide> slides;
  final double height;

  @override
  State<_FlipDeck> createState() => _FlipDeckState();
}

class _FlipDeckState extends State<_FlipDeck> with SingleTickerProviderStateMixin {
  late final AnimationController _motion = AnimationController(vsync: this, duration: const Duration(milliseconds: 560));
  Animation<double>? _tween;
  VoidCallback? _after;
  Timer? _auto;
  int _index = 0;
  double _turn = 0;
  bool _dragging = false;

  @override
  void initState() {
    super.initState();
    _motion.addListener(() {
      final tween = _tween;
      if (tween == null || !mounted) return;
      setState(() => _turn = tween.value);
    });
    _motion.addStatusListener((status) {
      if (status != AnimationStatus.completed) return;
      final done = _after;
      _after = null;
      done?.call();
    });
    _auto = Timer.periodic(const Duration(seconds: 5), (_) {
      if (!mounted || _dragging || _motion.isAnimating || _turn > 0.02) return;
      _finish();
    });
  }

  @override
  void dispose() {
    _auto?.cancel();
    _motion.dispose();
    super.dispose();
  }

  void _animateTurn(double target, {VoidCallback? done}) {
    _after = done;
    _tween = Tween<double>(begin: _turn, end: target).animate(CurvedAnimation(parent: _motion, curve: Curves.easeInOutCubic));
    _motion.forward(from: 0);
  }

  void _finish() {
    _animateTurn(1, done: () {
      if (!mounted) return;
      setState(() {
        _index = (_index + 1) % widget.slides.length;
        _turn = 0;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    final n = widget.slides.length;
    return Column(
      children: [
        SizedBox(
          height: widget.height,
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onHorizontalDragStart: (_) {
              _dragging = true;
              _motion.stop();
              _after = null;
            },
            onHorizontalDragUpdate: (d) {
              setState(() => _turn = (_turn - d.delta.dx / 260).clamp(0.0, 1.0));
            },
            onHorizontalDragEnd: (d) {
              _dragging = false;
              final v = d.primaryVelocity ?? 0;
              if (_turn > 0.32 || v < -500) {
                _finish();
              } else {
                _animateTurn(0);
              }
            },
            onHorizontalDragCancel: () {
              _dragging = false;
              _animateTurn(0);
            },
            child: LayoutBuilder(
              builder: (context, constraints) {
                final layers = <int>[2, 1, 0];
                return Stack(
                  alignment: Alignment.center,
                  clipBehavior: Clip.none,
                  children: [
                    for (final layer in layers) _deckCard(layer, constraints.maxWidth),
                  ],
                );
              },
            ),
          ),
        ),
        const SizedBox(height: 8),
        _Dots(
          count: n,
          index: _index,
          label: '${widget.slides[_index].kicker}  ${widget.slides[_index].title}',
          onTap: (page) {
            if (_motion.isAnimating) return;
            setState(() {
              _index = page.round() % n;
              _turn = 0;
            });
          },
        ),
      ],
    );
  }

  Widget _deckCard(int layer, double width) {
    final slot = layer - _turn;
    if (slot > 2.2 || slot < -1.05) return const SizedBox.shrink();
    final slide = widget.slides[(_index + layer) % widget.slides.length];
    final depth = slot.clamp(0.0, 2.2);
    final leaving = slot < 0;
    final y = leaving ? 0.0 : -14.0 * depth;
    final sc = leaving ? 1.0 : 1 - depth * 0.05;
    final fade = leaving ? (1 + slot).clamp(0.0, 1.0) : (1 - depth * 0.12).clamp(0.7, 1.0);
    return Transform(
      alignment: Alignment.center,
      transform: Matrix4.identity()
        ..setEntry(3, 2, 0.0014)
        ..translateByDouble(leaving ? slot * width * 1.08 : 0, y, leaving ? 12 : -24.0 * depth, 1)
        ..rotateY(leaving ? -slot * 0.42 : 0)
        ..scaleByDouble(sc, sc, 1, 1),
      child: Opacity(
        opacity: fade,
        child: SizedBox(
          width: width,
          height: widget.height - 28,
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(18),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.45),
                  blurRadius: 24,
                  offset: const Offset(0, 16),
                ),
                if (layer == 0)
                  BoxShadow(
                    color: _gold.withValues(alpha: 0.22 * (1 - _turn)),
                    blurRadius: 22,
                  ),
              ],
            ),
            child: _SlideFrame(slide: slide, showCaption: true),
          ),
        ),
      ),
    );
  }
}

class _Dots extends StatelessWidget {
  const _Dots({
    required this.count,
    required this.index,
    required this.label,
    required this.onTap,
  });

  final int count;
  final int index;
  final String label;
  final void Function(double page) onTap;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (var i = 0; i < count; i++)
          GestureDetector(
            onTap: () => onTap(i.toDouble()),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              width: i == index ? 18 : 6,
              height: 6,
              margin: const EdgeInsets.only(right: 6),
              decoration: BoxDecoration(
                color: i == index ? _rose : _faint,
                borderRadius: BorderRadius.circular(6),
              ),
            ),
          ),
        const Spacer(),
        Text(label, style: const TextStyle(color: _faint, fontSize: 11)),
      ],
    );
  }
}


class _SlideFrame extends StatelessWidget {
  const _SlideFrame({required this.slide, required this.showCaption});

  final _Slide slide;
  final bool showCaption;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _line),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(child: _AssetReel(key: ValueKey(slide.assets.join('|')), assets: slide.assets)),
          if (showCaption)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(slide.kicker, style: const TextStyle(color: _rose, fontSize: 22, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 6),
                  Text(slide.title, style: const TextStyle(color: _ink, fontSize: 16, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 6),
                  Text(slide.body, style: const TextStyle(color: _muted, fontSize: 13, height: 1.5)),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _AssetReel extends StatefulWidget {
  const _AssetReel({super.key, required this.assets});

  final List<String> assets;

  @override
  State<_AssetReel> createState() => _AssetReelState();
}

class _AssetReelState extends State<_AssetReel> {
  late final int _n = widget.assets.length;
  late final PageController _pages = PageController(initialPage: _n > 1 ? _n * 30 : 0);
  int _index = 0;
  Timer? _timer;

  int _real(int page) {
    final n = widget.assets.length;
    if (n <= 0) return 0;
    return ((page % n) + n) % n;
  }

  @override
  void initState() {
    super.initState();
    if (_n > 1) {
      _timer = Timer.periodic(const Duration(seconds: 4), (_) {
        if (!mounted || !_pages.hasClients || _pages.position.isScrollingNotifier.value) return;
        final current = _pages.page?.round() ?? _n * 30;
        _pages.animateToPage(current + 1, duration: const Duration(milliseconds: 480), curve: Curves.easeOutCubic);
      });
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    _pages.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final photo = widget.assets.length == 1
        ? Image.asset(
            widget.assets.first,
            fit: BoxFit.cover,
            errorBuilder: (_, __, ___) => const ColoredBox(color: Color(0xFF141418)),
          )
        : PageView.builder(
            controller: _pages,
            onPageChanged: (i) => setState(() => _index = _real(i)),
            itemBuilder: (_, i) => Image.asset(
              widget.assets[_real(i)],
              fit: BoxFit.cover,
              errorBuilder: (_, __, ___) => const ColoredBox(color: Color(0xFF141418)),
            ),
          );
    return Stack(
      fit: StackFit.expand,
      children: [
        photo,
        const DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [Color(0x0007070A), Color(0x9907070A)],
              stops: [0.45, 1],
            ),
          ),
        ),
        if (widget.assets.length > 1)
          Positioned(
            right: 12,
            bottom: 10,
            child: Row(
              children: [
                for (var i = 0; i < widget.assets.length; i++)
                  Container(
                    width: i == _index ? 14 : 5,
                    height: 5,
                    margin: const EdgeInsets.only(left: 4),
                    decoration: BoxDecoration(
                      color: i == _index ? _ink : _faint,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
              ],
            ),
          ),
      ],
    );
  }
}

class _CtaBand extends StatelessWidget {
  const _CtaBand({required this.cta, required this.onEnter});
  final String cta;
  final VoidCallback onEnter;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        gradient: const LinearGradient(
          colors: [Color(0x33E11D48), Color(0x2207070A)],
        ),
        border: Border.all(color: _line),
      ),
      child: Column(
        children: [
          const Text('READY', style: TextStyle(color: _gold, letterSpacing: 3, fontSize: 11)),
          const SizedBox(height: 10),
          const Text('把流通作业，做成可信任的系统', textAlign: TextAlign.center, style: TextStyle(color: _ink, fontSize: 22, fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          const Text('登录后即可进入工作台，查看库存、单据与追溯全貌。', textAlign: TextAlign.center, style: TextStyle(color: _muted)),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: onEnter,
            style: FilledButton.styleFrom(
              backgroundColor: _roseDeep,
              foregroundColor: Colors.white,
              minimumSize: const Size(180, 48),
              shape: const StadiumBorder(),
            ),
            child: Text(cta),
          ),
        ],
      ),
    );
  }
}

class _Foot extends StatelessWidget {
  const _Foot();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.fromLTRB(24, 8, 24, 40),
      child: Column(
        children: [
          Text('药企进销存 · Pharma IMS', style: TextStyle(color: _faint, fontSize: 12)),
          SizedBox(height: 4),
          Text('药品流通 · 批号库存 · 追溯码 © 2026', style: TextStyle(color: _faint, fontSize: 11)),
        ],
      ),
    );
  }
}

class _Reveal extends StatelessWidget {
  const _Reveal({required this.animation, required this.child});
  final Animation<double> animation;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(begin: const Offset(0, 0.12), end: Offset.zero).animate(animation),
        child: child,
      ),
    );
  }
}
