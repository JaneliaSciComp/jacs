// Fragment shader for all filtering applied by volume brick.
#version 120
uniform sampler3D signalTexture;

// Borrowed these from the ColorFrag shader.  Thanks, Christopher Bruns.
uniform vec3 channel_color[4]; // color for each channel, initially red, green, blue, gray
uniform vec4 channel_gamma; // brightness for each channel
uniform vec4 channel_min;
uniform vec4 channel_scale;
uniform int channel_count;

vec4 chooseColor() 
{
    vec4 in_color = texture3D(signalTexture, gl_TexCoord[0].xyz);

/*
  Uncommenting this yields a stack of black and white slabs.

if ( 0 == 0 )
{
return in_color;
}
*/

    // Two color images are loaded as luminance/alpha, so look in alpha
    // for second intensity, not in green.
    if (channel_count == 2)
        in_color.g = in_color.a;
    vec3 out_color = vec3(0, 0, 0);
    for (int c = 0; c < channel_count; ++c) 
    {
        float i = in_color[c]; // intensity
        i -= channel_min[c]; // apply black level
        i *= channel_scale[c]; // apply white level
        i = clamp(i, 0, 1); // avoid extreme numbers in sum
        i = pow(i, channel_gamma[c]); // apply gamma correction
        // out_color += i * channel_color[c]; // apply channel color
        out_color = max(out_color, i * channel_color[c]); // apply channel color
    }
    // Final sRGB color correction, because JOGL 2.1 won't do it.
    // vec3 srgb = pow(out_color.rgb, 0.46); // No vec pow in glsl 1.20?
    vec3 srgb = vec3(pow(out_color.r, 0.46), pow(out_color.g, 0.46), pow(out_color.b, 0.46));
    return vec4(srgb, 1.0);
}

void main()
{
    gl_FragColor = chooseColor();
}

