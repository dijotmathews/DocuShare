// Encodes the frames written by record-demo-mobile.mjs into an H.264 MP4 (macOS, no ffmpeg needed).
// Usage: swift encode-mp4.swift <frames-dir> <output.mp4>
// <frames-dir>/frames.ffconcat lists each JPEG frame and how long it stays on screen; the
// output is constant 30 fps, the frame shown at each moment being the latest one captured.
import AVFoundation
import CoreGraphics
import Foundation
import ImageIO

let args = CommandLine.arguments
guard args.count == 3 else {
    FileHandle.standardError.write("Usage: swift encode-mp4.swift <frames-dir> <output.mp4>\n".data(using: .utf8)!)
    exit(2)
}
let framesDir = URL(fileURLWithPath: args[1])
let output = URL(fileURLWithPath: args[2])
let fps: Int32 = 30

// file 'f00001.jpg' / duration 0.040 pairs
var entries: [(file: String, duration: Double)] = []
let list = try String(contentsOf: framesDir.appendingPathComponent("frames.ffconcat"), encoding: .utf8)
for line in list.split(separator: "\n") {
    if line.hasPrefix("file ") {
        entries.append((String(line.dropFirst(6).dropLast()), 0))
    } else if line.hasPrefix("duration "), !entries.isEmpty {
        entries[entries.count - 1].duration = Double(line.dropFirst(9)) ?? 0
    }
}
guard !entries.isEmpty else { fatalError("no frames listed") }

func image(_ name: String) -> CGImage {
    let url = framesDir.appendingPathComponent(name)
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
          let img = CGImageSourceCreateImageAtIndex(source, 0, nil) else { fatalError("cannot read \(name)") }
    return img
}

// Frame size from the last frame (the closing title card, always fully rendered)
let reference = image(entries[entries.count - 1].file)
let width = reference.width - reference.width % 2, height = reference.height - reference.height % 2

try? FileManager.default.removeItem(at: output)
let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoAverageBitRateKey: 3_500_000,
        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
        AVVideoMaxKeyFrameIntervalKey: Int(fps) * 2,
    ],
])
input.expectsMediaDataInRealTime = false
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA,
    kCVPixelBufferWidthKey as String: width,
    kCVPixelBufferHeightKey as String: height,
])
writer.add(input)
writer.startWriting()
writer.startSession(atSourceTime: .zero)

func pixelBuffer(_ img: CGImage) -> CVPixelBuffer {
    var buffer: CVPixelBuffer?
    CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &buffer)
    let pb = buffer!
    CVPixelBufferLockBaseAddress(pb, [])
    let ctx = CGContext(data: CVPixelBufferGetBaseAddress(pb), width: width, height: height, bitsPerComponent: 8,
                        bytesPerRow: CVPixelBufferGetBytesPerRow(pb), space: CGColorSpaceCreateDeviceRGB(),
                        bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue)!
    ctx.interpolationQuality = .high
    ctx.draw(img, in: CGRect(x: 0, y: 0, width: width, height: height))
    CVPixelBufferUnlockBaseAddress(pb, [])
    return pb
}

// Start time of each captured frame
var starts: [Double] = []
var t = 0.0
for e in entries { starts.append(t); t += e.duration }
let total = t
let frameCount = Int((total * Double(fps)).rounded(.up))

var current = -1
var currentBuffer: CVPixelBuffer?
var index = 0
for n in 0..<frameCount {
    let time = Double(n) / Double(fps)
    while index + 1 < starts.count && starts[index + 1] <= time { index += 1 }
    if index != current {
        currentBuffer = pixelBuffer(image(entries[index].file))
        current = index
    }
    while !input.isReadyForMoreMediaData { Thread.sleep(forTimeInterval: 0.005) }
    adaptor.append(currentBuffer!, withPresentationTime: CMTime(value: CMTimeValue(n), timescale: fps))
}
input.markAsFinished()
let done = DispatchSemaphore(value: 0)
writer.finishWriting { done.signal() }
done.wait()
if writer.status != .completed {
    fatalError("encoding failed: \(writer.error?.localizedDescription ?? "unknown")")
}
print(String(format: "%@ (%dx%d, %.1f s)", output.path, width, height, total))
