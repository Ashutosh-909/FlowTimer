import SwiftUI

private struct Star: Identifiable {
    let id = UUID()
    let x: CGFloat
    let y: CGFloat
    let size: CGFloat
    let opacity: Double
}

/// Decorative star-field background matching the Android SpaceBackground theme.
struct StarFieldView: View {
    private let stars: [Star] = (0..<80).map { _ in
        Star(
            x: CGFloat.random(in: 0...1),
            y: CGFloat.random(in: 0...1),
            size: CGFloat.random(in: 1...3),
            opacity: Double.random(in: 0.2...0.8)
        )
    }

    var body: some View {
        GeometryReader { geo in
            ForEach(stars) { star in
                Circle()
                    .fill(Color.white.opacity(star.opacity))
                    .frame(width: star.size, height: star.size)
                    .position(
                        x: star.x * geo.size.width,
                        y: star.y * geo.size.height
                    )
            }
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}
