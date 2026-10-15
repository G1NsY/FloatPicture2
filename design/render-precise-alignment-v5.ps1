Add-Type -AssemblyName System.Drawing

$basePath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-single-phone-v5-base.png'
$screenPath = 'C:\Users\AAA\AppData\Local\Temp\codex-clipboard-18dad71b-6eb4-408f-8861-51453d5779ce.jpg'
$outPath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-single-phone-v5.png'

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
    Text $label 60 $y 28 '#003E86' $true
    $pen = [System.Drawing.Pen]::new((Color '#4FA2F7'), 3)
    $g.DrawLine($pen, 61, ($y+39), 408, ($y+39))
    $pen.Dispose()
}
function Row([string] $label, [string] $value, [float] $y, [float] $progress) {
    Text $label 69 $y 20 '#254B72'
    TextRight $value 406 $y 20 '#003978'
    $sy = $y + 44
    $track = [System.Drawing.Pen]::new((Color '#A9C7E3'), 5)
    $active = [System.Drawing.Pen]::new((Color '#2488F0'), 5)
    $g.DrawLine($track, 110, $sy, 357, $sy)
    $endX = [int](110 + 247 * $progress)
    $g.DrawLine($active, 110, $sy, $endX, $sy)
    $track.Dispose(); $active.Dispose()
    $blue = Brush '#2184EC'
    $white = Brush '#FFFFFF'
    $g.FillEllipse($blue, ($endX-9), ($sy-9), 18, 18)
    $g.FillEllipse($blue, 63, ($sy-15), 30, 30)
    $g.FillEllipse($blue, 373, ($sy-15), 30, 30)
    $buttonFont = Font 21
    $g.DrawString('−', $buttonFont, $white, 69, ($sy-18))
    $g.DrawString('+', $buttonFont, $white, 377, ($sy-18))
    $buttonFont.Dispose(); $blue.Dispose(); $white.Dispose()
}

# Direct-on-poster parameter typography: there is deliberately no phone or
# surrounding settings card on the left.
Heading '尺寸' 438
Row '宽度' '2013 px' 486 0.54
Row '高度' '4025 px' 552 0.70
Heading '位置' 628
Row 'X 坐标' '-957' 676 0.28
Row 'Y 坐标' '-5' 742 0.49
Heading '其他' 818
Row '角度' '0°' 866 0.50
Row '透明度' '0.5' 932 0.50

# Preserve the supplied screenshot rather than regenerating its contents.
# It is fitted uniformly inside the front-facing phone with the whole source
# visible; the remaining black space blends with the camera UI.
$phoneClip = RoundedPath 475 407 421 963 19
$g.SetClip($phoneClip)
$black = Brush '#02050A'
$g.FillRectangle($black, 475, 407, 421, 963)
$black.Dispose()
$width = 416
$height = [int][Math]::Round($width * $screen.Height / $screen.Width)
$top = [int][Math]::Round(407 + (963 - $height) / 2)
$dest = [System.Drawing.Rectangle]::new(477, $top, $width, $height)
$src = [System.Drawing.Rectangle]::new(0, 0, $screen.Width, $screen.Height)
$g.DrawImage($screen, $dest, $src, [System.Drawing.GraphicsUnit]::Pixel)
$g.ResetClip(); $phoneClip.Dispose()

$canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $screen.Dispose(); $canvas.Dispose()
Write-Output $outPath
