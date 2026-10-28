"""Rigid constant-length linkage tracks, evaluated from bay width rather than interpolated endpoints."""
import math
LENGTH=.42
OPEN=.36
CLOSED=.04
BAYS=5

def values(spec,clip,f):
    smooth=lambda x:x*x*(3-2*x)
    if clip in ('PACKED','THROWN','IMPACT'):progress=0
    elif clip=='DEPLOYING':progress=smooth(f)
    elif clip=='DESTROYED':progress=1-.6*smooth(f)
    elif clip=='OPERATING':progress=1-.02*max(0,math.sin(math.pi*f))
    elif clip=='DAMAGED':progress=1-.012*math.sin(math.pi*f)**2
    elif clip=='EXPIRING':progress=1-.01*math.sin(math.pi*f)**2
    else:progress=1
    width=CLOSED+(OPEN-CLOSED)*progress;height=math.sqrt(LENGTH*LENGTH-width*width)
    full_height=math.sqrt(LENGTH*LENGTH-OPEN*OPEN);center=.25+.17*progress
    kind=spec['kind'];index=spec['index'];rotation=None
    if kind in ('bar','center','rail'):
        dx=(index-(BAYS-1)/2)*(width-OPEN)
        dz=(.44+.40*progress-.84) if kind=='rail' else center-.42
        if kind=='bar':rotation=-spec['sign']*(math.atan2(height,width)-math.atan2(full_height,OPEN))
    else:
        dx=(index-BAYS/2)*(width-OPEN)
        dz=center+spec['level']*height/2-(.42+spec['level']*full_height/2) if kind=='guide' else 0
    return (dx,0,dz),rotation
