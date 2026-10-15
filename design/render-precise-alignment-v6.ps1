Add-Type -AssemblyName System.Drawing

$basePath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-single-phone-v6-base.png'
$screenPath = 'C:\Users\AAA\AppData\Local\Temp\codex-clipboard-18dad71b-6eb4-408f-8861-51453d5779ce.jpg'
$outPath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-single-phone-v6.png'

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
function Text([string] $value, [float] $x, [float] $y, [float] $size, [string] $hex, [bool] $bold = $false) {
    $style = if ($bold) { [System.Drawing.FontStyle]::Bold } else { [System.Drawing.FontStyle]::Regular }
    $font = Font $size $style
    $brush = Brush $hex
    $g.DrawString($value, $font, $brush, $x, $y)
    $brush.Dispose(); $font.Dispose()
}
function TextRight([string] $value, [float] $right, [float] $y, [float] $size, [string] $hex) {
    $font = Font $size ([System.Drawing.FontStyle]::Bold)
    $width = $g.MeasureString($value, $font).Width
    $brush = Brush $hex
    $g.DrawString($value, $font, $brush, ($right-$width), $y)
    $brush.Dispose(); $font.Dispose()
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
function Heading([string] $label, [float] $y) {
    Text $label 60 $y 26 '#003E86' $true
    $pen = [System.Drawing.Pen]::new((Color '#4FA2F7'), 3)
    $g.DrawLine($pen, 61, ($y+36), 416, ($y+36))
    $pen.Dispose()
}
function Row([string] $label, [string] $value, [float] $y) {
    Text $label 69 $y 21 '#254B72'
    $blue = Brush '#2184EC'
    $white = Brush '#FFFFFF'
    $g.FillEllipse($blue, 214, ($y-1), 34, 34)
    $g.FillEllipse($blue, 392, ($y-1), 34, 34)
    $buttonFont = Font 29 ([System.Drawing.FontStyle]::Bold)
    $g.DrawString('‹', $buttonFont, $white, 221, ($y-9))
    $g.DrawString('›', $buttonFont, $white, 400, ($y-9))
    $valueFont = Font 26 ([System.Drawing.FontStyle]::Bold)
    $valueWidth = $g.MeasureString($value, $valueFont).Width
    $valueBrush = Brush '#003978'
    $g.DrawString($value, $valueFont, $valueBrush, (321-$valueWidth/2), ($y-4))
    $valueBrush.Dispose(); $valueFont.Dispose()
    $buttonFont.Dispose(); $blue.Dispose(); $white.Dispose()
}

# Direct-on-poster parameter typography: there is deliberately no phone or
# surrounding settings card on the left.
Heading '尺寸' 415
Row '宽度' '2013 px' 461
Row '高度' '4025 px' 514
Heading '位置' 568
Row 'X 坐标' '-957' 614
Row 'Y 坐标' '-5' 667
Heading '其他' 721
Row '角度' '0°' 767
Row '透明度' '0.5' 820

# Preserve the supplied screenshot rather than regenerating its contents.
# It is fitted uniformly inside the front-facing phone with the whole source
# visible; the remaining black space blends with the camera UI.
$phoneClip = RoundedPath 479 439 415 845 18
$g.SetClip($phoneClip)
$overlap = [System.Drawing.Drawing2D.GraphicsPath]::new()
$points = [System.Drawing.Point[]]@(
    [System.Drawing.Point]::new(0, 1180),
    [System.Drawing.Point]::new(485, 1180),
    [System.Drawing.Point]::new(530, 1285),
    [System.Drawing.Point]::new(0, 1285)
)
$overlap.AddPolygon($points)
$overlapRegion = [System.Drawing.Region]::new($overlap)
$g.ExcludeClip($overlapRegion)
$overlapRegion.Dispose()
$overlap.Dispose()
$width = 404
$height = [int][Math]::Round($width * $screen.Height / $screen.Width)
$top = [int][Math]::Round(439 + (845 - $height) / 2)
$dest = [System.Drawing.Rectangle]::new(485, $top, $width, $height)
$src = [System.Drawing.Rectangle]::new(0, 0, $screen.Width, $screen.Height)
$g.DrawImage($screen, $dest, $src, [System.Drawing.GraphicsUnit]::Pixel)
$g.ResetClip(); $phoneClip.Dispose()

$canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $screen.Dispose(); $canvas.Dispose()
Write-Output $outPath
