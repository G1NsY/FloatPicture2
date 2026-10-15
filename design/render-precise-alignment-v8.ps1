Add-Type -AssemblyName System.Drawing

$basePath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-reference-card-v8-base.png'
$screenPath = 'C:\Users\AAA\AppData\Local\Temp\codex-clipboard-18dad71b-6eb4-408f-8861-51453d5779ce.jpg'
$outPath = 'C:\Users\AAA\Documents\FloatPiture_App优化\design\store-poster-precise-alignment-reference-card-v8.png'

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
function DrawTextCenter([string] $value, [float] $cx, [float] $y, [float] $size, [string] $hex) {
    $font = Font $size ([System.Drawing.FontStyle]::Bold)
    $brush = Brush $hex
    $width = $g.MeasureString($value, $font).Width
    $g.DrawString($value, $font, $brush, ($cx-$width/2), $y)
    $brush.Dispose(); $font.Dispose()
}
function CircleArrow([float] $cx, [float] $cy, [string] $direction) {
    $blue = Brush '#1883E8'
    $white = Brush '#FFFFFF'
    $g.FillEllipse($blue, ($cx-19), ($cy-19), 38, 38)
    $font = Font 28 ([System.Drawing.FontStyle]::Bold)
    $glyph = switch ($direction) { 'left' {'‹'} 'right' {'›'} 'up' {'⌃'} 'down' {'⌄'} }
    $x = switch ($direction) { 'left' {$cx-8} 'right' {$cx-6} 'up' {$cx-10} 'down' {$cx-10} }
    $y = switch ($direction) { 'left' {$cy-17} 'right' {$cy-17} 'up' {$cy-23} 'down' {$cy-23} }
    $g.DrawString($glyph, $font, $white, $x, $y)
    $font.Dispose(); $white.Dispose(); $blue.Dispose()
}
function Badge([float] $cx, [float] $cy, [string] $kind) {
    $shadow = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(55, 29, 103, 177))
    $white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(247, 255, 255, 255))
    $outline = [System.Drawing.Pen]::new((Color '#B7D9FA'), 2)
    $icon = [System.Drawing.Pen]::new((Color '#0A57B3'), 4)
    $icon.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $icon.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.FillEllipse($shadow, ($cx-25), ($cy-21), 54, 54)
    $g.FillEllipse($white, ($cx-27), ($cy-27), 54, 54)
    $g.DrawEllipse($outline, ($cx-27), ($cy-27), 54, 54)
    switch ($kind) {
        'move' {
            $g.DrawLine($icon, $cx, ($cy-14), $cx, ($cy+14))
            $g.DrawLine($icon, ($cx-14), $cy, ($cx+14), $cy)
            $g.DrawLine($icon, $cx, ($cy-14), ($cx-5), ($cy-8))
            $g.DrawLine($icon, $cx, ($cy-14), ($cx+5), ($cy-8))
            $g.DrawLine($icon, $cx, ($cy+14), ($cx-5), ($cy+8))
            $g.DrawLine($icon, $cx, ($cy+14), ($cx+5), ($cy+8))
            $g.DrawLine($icon, ($cx-14), $cy, ($cx-8), ($cy-5))
            $g.DrawLine($icon, ($cx-14), $cy, ($cx-8), ($cy+5))
            $g.DrawLine($icon, ($cx+14), $cy, ($cx+8), ($cy-5))
            $g.DrawLine($icon, ($cx+14), $cy, ($cx+8), ($cy+5))
        }
        'scale' {
            $g.DrawLine($icon, ($cx-11), ($cy+11), ($cx+11), ($cy-11))
            $g.DrawLine($icon, ($cx+11), ($cy-11), ($cx+3), ($cy-11))
            $g.DrawLine($icon, ($cx+11), ($cy-11), ($cx+11), ($cy-3))
            $g.DrawLine($icon, ($cx-11), ($cy+11), ($cx-3), ($cy+11))
            $g.DrawLine($icon, ($cx-11), ($cy+11), ($cx-11), ($cy+3))
        }
        'rotate' {
            $g.DrawArc($icon, ($cx-13), ($cy-13), 26, 26, -75, 295)
            $g.DrawLine($icon, ($cx+12), ($cy-5), ($cx+13), ($cy+5))
            $g.DrawLine($icon, ($cx+13), ($cy+5), ($cx+4), ($cy+2))
        }
    }
    $icon.Dispose(); $outline.Dispose(); $white.Dispose(); $shadow.Dispose()
}

# Keep the complete user-supplied screenshot, scaling it uniformly into the
# phone. Leave the boy in front of the small lower-left overlap.
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
$overlapRegion.Dispose(); $overlap.Dispose()
$width = 404
$height = [int][Math]::Round($width * $screen.Height / $screen.Width)
$top = [int][Math]::Round(439 + (845 - $height) / 2)
$dest = [System.Drawing.Rectangle]::new(485, $top, $width, $height)
$src = [System.Drawing.Rectangle]::new(0, 0, $screen.Width, $screen.Height)
$g.DrawImage($screen, $dest, $src, [System.Drawing.GraphicsUnit]::Pixel)
$g.ResetClip(); $phoneClip.Dispose()

# Only two prominent dimension values, one on each axis.
CircleArrow 204 545 'left'
DrawTextCenter '2013px' 337 513 40 '#003A81'
CircleArrow 468 545 'right'
CircleArrow 110 653 'up'
$state = $g.Save()
$g.TranslateTransform(78, 844)
$g.RotateTransform(-90)
$verticalFont = Font 39 ([System.Drawing.FontStyle]::Bold)
$verticalBrush = Brush '#003A81'
$g.DrawString('4025px', $verticalFont, $verticalBrush, 0, 0)
$verticalBrush.Dispose(); $verticalFont.Dispose()
$g.Restore($state)
CircleArrow 110 887 'down'

# The three editing controls from the reference layout.
Badge 350 614 'move'
Badge 201 791 'scale'
Badge 478 790 'rotate'

$canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $screen.Dispose(); $canvas.Dispose()
Write-Output $outPath
