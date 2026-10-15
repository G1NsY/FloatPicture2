Add-Type -AssemblyName System.Drawing

$basePath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-two-phones-v3.png'
$screenPath = 'C:\Users\AAA\AppData\Local\Temp\codex-clipboard-18dad71b-6eb4-408f-8861-51453d5779ce.jpg'
$outPath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-two-phones-v4.png'

$canvas = [System.Drawing.Bitmap]::FromFile($basePath)
$screen = [System.Drawing.Bitmap]::FromFile($screenPath)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

function Color([string] $hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
function Brush([string] $hex) { [System.Drawing.SolidBrush]::new((Color $hex)) }
function Font([float] $size, [System.Drawing.FontStyle] $style = [System.Drawing.FontStyle]::Regular) {
    [System.Drawing.Font]::new('Microsoft YaHei', $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
}
function RoundedPath([float] $x, [float] $y, [float] $w, [float] $h, [float] $radius) {
    $p = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $d = $radius * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc(($x+$w-$d), $y, $d, $d, 270, 90)
    $p.AddArc(($x+$w-$d), ($y+$h-$d), $d, $d, 0, 90)
    $p.AddArc($x, ($y+$h-$d), $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}
function Text([string] $value, [float] $x, [float] $y, [float] $size, [string] $hex, [bool] $bold = $false) {
    $style = if ($bold) { [System.Drawing.FontStyle]::Bold } else { [System.Drawing.FontStyle]::Regular }
    $font = Font $size $style
    $brush = Brush $hex
    $g.DrawString($value, $font, $brush, $x, $y)
    $brush.Dispose(); $font.Dispose()
}
function TextRight([string] $value, [float] $right, [float] $y, [float] $size, [string] $hex) {
    $font = Font $size
    $width = $g.MeasureString($value, $font).Width
    $brush = Brush $hex
    $g.DrawString($value, $font, $brush, ($right-$width), $y)
    $brush.Dispose(); $font.Dispose()
}
function DrawRow([float] $y, [string] $label, [string] $value, [float] $progress) {
    $linePen = [System.Drawing.Pen]::new((Color '#E7EEF6'), 1)
    $g.DrawLine($linePen, 122, ($y+65), 413, ($y+65))
    $linePen.Dispose()
    Text $label 128 $y 17 '#344B65'
    TextRight $value 409 $y 18 '#0C376B'
    $trackY = $y + 44
    $track = [System.Drawing.Pen]::new((Color '#CBD9E8'), 4)
    $active = [System.Drawing.Pen]::new((Color '#338CF2'), 4)
    $g.DrawLine($track, 177, $trackY, 363, $trackY)
    $endX = [int](177 + 186 * $progress)
    $g.DrawLine($active, 177, $trackY, $endX, $trackY)
    $track.Dispose(); $active.Dispose()
    $blue = Brush '#2485EA'
    $white = Brush '#FFFFFF'
    $g.FillEllipse($blue, ($endX-8), ($trackY-8), 16, 16)
    $g.FillEllipse($blue, 129, ($trackY-12), 24, 24)
    $g.FillEllipse($blue, 386, ($trackY-12), 24, 24)
    $whiteFont = Font 18
    $g.DrawString('−', $whiteFont, $white, 133, ($trackY-15))
    $g.DrawString('+', $whiteFont, $white, 390, ($trackY-15))
    $whiteFont.Dispose(); $white.Dispose(); $blue.Dispose()
}

# Keep the existing phones and character. Replace the left phone's screen with an
# intentionally simplified settings concept, without painting over the boy.
$leftClip = RoundedPath 102 479 328 704 13
$g.SetClip($leftClip)
$paper = Brush '#F8FBFF'
$g.FillRectangle($paper, 103, 480, 328, 584)
$paper.Dispose()
$blueBar = Brush '#1974D2'
$g.FillRectangle($blueBar, 103, 479, 328, 60)
$blueBar.Dispose()
Text '精细调整' 124 492 25 '#FFFFFF' $true
Text '尺寸' 123 546 21 '#1974D2' $true
DrawRow 580 '宽度' '2013 px' 0.54
DrawRow 646 '高度' '4025 px' 0.70
Text '位置' 123 714 21 '#1974D2' $true
DrawRow 748 'X 坐标' '-957' 0.28
DrawRow 814 'Y 坐标' '-5' 0.49
Text '其他' 123 882 21 '#1974D2' $true
DrawRow 916 '角度' '0°' 0.50
DrawRow 982 '透明度' '0.5' 0.50
$g.ResetClip(); $leftClip.Dispose()

# Paste the source screenshot itself into the front-facing right phone. Keep its
# original aspect ratio and pixels aside from uniform downscaling.
$rightClip = RoundedPath 475 413 419 958 18
$g.SetClip($rightClip)
$black = Brush '#03070C'
$g.FillRectangle($black, 475, 413, 419, 958)
$black.Dispose()
$targetWidth = 414
$targetHeight = [int][Math]::Round($targetWidth * $screen.Height / $screen.Width)
$targetY = [int][Math]::Round(413 + (958 - $targetHeight) / 2)
$targetRect = [System.Drawing.Rectangle]::new(478, $targetY, $targetWidth, $targetHeight)
$sourceRect = [System.Drawing.Rectangle]::new(0, 0, $screen.Width, $screen.Height)
$g.DrawImage($screen, $targetRect, $sourceRect, [System.Drawing.GraphicsUnit]::Pixel)
$g.ResetClip(); $rightClip.Dispose()

$canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $screen.Dispose(); $canvas.Dispose()
Write-Output $outPath
